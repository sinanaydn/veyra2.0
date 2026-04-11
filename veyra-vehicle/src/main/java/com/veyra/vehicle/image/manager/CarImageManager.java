package com.veyra.vehicle.image.manager;

import com.veyra.core.storage.StorageService;
import com.veyra.core.storage.StoredFile;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.rules.CarRules;
import com.veyra.vehicle.image.dto.request.ReorderImageItem;
import com.veyra.vehicle.image.dto.request.ReorderImagesRequest;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import com.veyra.vehicle.image.entity.CarImage;
import com.veyra.vehicle.image.mapper.CarImageMapper;
import com.veyra.vehicle.image.repository.CarImageRepository;
import com.veyra.vehicle.image.rules.CarImageRules;
import com.veyra.vehicle.image.service.CarImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CarImage iş akışlarının implementation'ı.
 *
 * Modüller arası bağımlılık:
 *  - CarRules (car'ın var olduğunu doğrular — cross-domain ama aynı modül içi)
 *  - StorageService (veyra-core → object storage soyutlaması)
 *
 * Tüm write metotları {@code @Transactional}, read metotları {@code readOnly=true}.
 * Upload sırasında S3'e dosya önce gider, sonra DB kaydı oluşur:
 *   - Eğer S3 upload başarısızsa StorageException fırlar → DB'de hayalet kayıt kalmaz.
 *   - Eğer DB kayıt aşamasında hata olursa → S3'te orphan obje kalabilir ama
 *     daha güvenli, çünkü orphan object az problem, orphan DB row büyük problem.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CarImageManager implements CarImageService {

    private final CarImageRepository carImageRepository;
    private final CarImageRules      carImageRules;
    private final CarRules           carRules;
    private final CarImageMapper     carImageMapper;
    private final StorageService     storageService;

    // ------------------------------------------------------------------ //
    //  Upload                                                             //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public CarImageResponse upload(Long carId, MultipartFile file) {
        Car car = carRules.getByIdOrThrow(carId);

        carImageRules.checkFileNotEmpty(file);
        carImageRules.checkFileSize(file);
        carImageRules.checkContentType(file);
        carImageRules.checkMaxImagesPerCar(carId);

        StoredFile stored = storageService.upload(file, "cars/" + carId);

        int nextOrder = carImageRepository.findMaxDisplayOrderByCarId(carId) + 1;
        boolean makePrimary = (carImageRepository.countByCarId(carId) == 0);

        CarImage image = CarImage.builder()
                .car(car)
                .storageKey(stored.storageKey())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .displayOrder(nextOrder)
                .isPrimary(makePrimary)
                .build();

        CarImage saved = carImageRepository.save(image);
        log.info("CarImage: uploaded id={} for car={}, primary={}", saved.getId(), carId, makePrimary);

        return carImageMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Read                                                               //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional(readOnly = true)
    public List<CarImageResponse> getByCarId(Long carId) {
        carRules.getByIdOrThrow(carId); // araç var mı?
        List<CarImage> images = carImageRepository.findByCarIdOrderByDisplayOrderAsc(carId);
        return carImageMapper.toResponseList(images);
    }

    // ------------------------------------------------------------------ //
    //  Delete                                                             //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public void delete(Long carId, Long imageId) {
        carRules.getByIdOrThrow(carId);
        CarImage image = carImageRules.getByIdForCarOrThrow(imageId, carId);

        boolean wasPrimary = image.isPrimary();
        String storageKey = image.getStorageKey();

        // Soft delete DB kaydı
        image.setDeleted(true);
        image.setPrimary(false);
        carImageRepository.save(image);

        // Storage'dan anında sil (senin kararın)
        try {
            storageService.delete(storageKey);
        } catch (Exception e) {
            // Storage delete başarısız olsa bile DB soft delete zaten yapıldı.
            // Orphan obje storage'da kalır ama kullanıcıya görünmez. Log'la + devam et.
            log.warn("Storage delete başarısız, orphan key kalabilir: {} — {}", storageKey, e.getMessage());
        }

        // Eğer kapaktıysa, kalanlar arasından yeni kapak belirle (displayOrder'ı en düşük)
        if (wasPrimary) {
            List<CarImage> remaining = carImageRepository.findByCarIdOrderByDisplayOrderAsc(carId);
            if (!remaining.isEmpty()) {
                CarImage newPrimary = remaining.get(0);
                newPrimary.setPrimary(true);
                carImageRepository.save(newPrimary);
                log.info("CarImage: new primary set to id={} for car={}", newPrimary.getId(), carId);
            }
        }

        log.info("CarImage: deleted id={} for car={}", imageId, carId);
    }

    // ------------------------------------------------------------------ //
    //  Primary                                                            //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public CarImageResponse setPrimary(Long carId, Long imageId) {
        carRules.getByIdOrThrow(carId);
        CarImage newPrimary = carImageRules.getByIdForCarOrThrow(imageId, carId);

        // Zaten primary'yse boşa save yapmayalım
        if (newPrimary.isPrimary()) {
            return carImageMapper.toResponse(newPrimary);
        }

        // Eski primary'yi kaldır (varsa)
        carImageRepository.findFirstByCarIdAndIsPrimaryTrue(carId).ifPresent(old -> {
            old.setPrimary(false);
            carImageRepository.save(old);
        });

        newPrimary.setPrimary(true);
        CarImage saved = carImageRepository.save(newPrimary);

        return carImageMapper.toResponse(saved);
    }

    // ------------------------------------------------------------------ //
    //  Reorder                                                            //
    // ------------------------------------------------------------------ //

    @Override
    @Transactional
    public List<CarImageResponse> reorder(Long carId, ReorderImagesRequest request) {
        carRules.getByIdOrThrow(carId);

        // Araca ait mevcut tüm görseller — id → entity
        List<CarImage> current = carImageRepository.findByCarIdOrderByDisplayOrderAsc(carId);
        Map<Long, CarImage> byId = current.stream()
                .collect(Collectors.toMap(CarImage::getId, Function.identity()));

        // Her bir item'ın bu araca ait olduğunu doğrula (ownership), sonra order'ı güncelle
        for (ReorderImageItem item : request.getItems()) {
            CarImage img = byId.get(item.getImageId());
            if (img == null) {
                // id ya başka araca ait ya da yok — ikisinde de ownership ihlali sayılır
                carImageRules.getByIdForCarOrThrow(item.getImageId(), carId); // fırlatır
            } else {
                img.setDisplayOrder(item.getDisplayOrder());
            }
        }

        carImageRepository.saveAll(current);

        // Yeni sıraya göre response dön
        List<CarImage> reordered = current.stream()
                .sorted(Comparator.comparingInt(CarImage::getDisplayOrder))
                .toList();
        return carImageMapper.toResponseList(reordered);
    }
}
