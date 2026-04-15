package com.veyra.vehicle.car.manager;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.core.response.PageResponse;
import com.veyra.vehicle.car.dto.request.CarFilterRequest;
import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.car.mapper.CarMapper;
import com.veyra.vehicle.car.repository.CarRepository;
import com.veyra.vehicle.car.rules.CarRules;
import com.veyra.vehicle.car.service.CarService;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import com.veyra.vehicle.image.entity.CarImage;
import com.veyra.vehicle.image.mapper.CarImageMapper;
import com.veyra.vehicle.image.repository.CarImageRepository;
import com.veyra.vehicle.car.specification.CarSpecification;
import com.veyra.vehicle.model.rules.CarModelRules;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarManager implements CarService {

    private final CarRepository       carRepository;
    private final CarRules            carRules;
    private final CarModelRules       carModelRules;
    private final CarMapper           carMapper;
    private final CarImageRepository  carImageRepository;
    private final CarImageMapper      carImageMapper;

    @Override
    @Transactional
    public CarResponse create(CreateCarRequest request) {
        var model = carModelRules.getByIdOrThrow(request.getModelId());

        var car = Car.builder()
                .model(model)
                .year(request.getYear())
                .doors(request.getDoors())
                .baggages(request.getBaggages())
                .dailyPrice(request.getDailyPrice())
                .fuelType(request.getFuelType())
                .transmission(request.getTransmission())
                .seats(request.getSeats())
                .color(request.getColor())
                .mileage(request.getMileage())
                .description(request.getDescription())
                .build();

        CarResponse response = carMapper.toResponse(carRepository.save(car));
        // Yeni araç — henüz görsel yok
        response.setImages(Collections.emptyList());
        return response;
    }

    @Override
    @Transactional
    public CarResponse update(Long id, UpdateCarRequest request) {
        var car   = carRules.getByIdOrThrow(id);
        var model = carModelRules.getByIdOrThrow(request.getModelId());

        car.setModel(model);
        car.setYear(request.getYear());
        car.setDoors(request.getDoors());
        car.setBaggages(request.getBaggages());
        car.setDailyPrice(request.getDailyPrice());
        car.setFuelType(request.getFuelType());
        car.setTransmission(request.getTransmission());
        car.setSeats(request.getSeats());
        car.setColor(request.getColor());
        car.setMileage(request.getMileage());
        car.setDescription(request.getDescription());
        car.setStatus(request.getStatus());

        CarResponse response = carMapper.toResponse(carRepository.save(car));
        enrichWithImages(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public CarResponse getById(Long id) {
        var car = carRepository.findByIdWithModelAndBrand(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_NOT_FOUND, "Araç bulunamadı: " + id));
        CarResponse response = carMapper.toResponse(car);
        enrichWithImages(response);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CarResponse> getAll(Pageable pageable) {
        var page = carRepository.findAllWithModelAndBrand(pageable).map(carMapper::toResponse);
        enrichListWithImages(page.getContent());
        return new PageResponse<>(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CarResponse> search(CarFilterRequest filter, Pageable pageable) {
        Specification<Car> spec = CarSpecification.withFilters(filter);
        var page = carRepository.findAll(spec, pageable).map(carMapper::toResponse);
        enrichListWithImages(page.getContent());
        return new PageResponse<>(page);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var car = carRules.getByIdOrThrow(id);
        carRules.checkIfCarCanBeDeleted(car);
        car.setDeleted(true);
        carRepository.save(car);
    }

    @Override
    @Transactional
    public void markAsRented(Long carId) {
        var car = carRules.getByIdOrThrow(carId);
        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
    }

    @Override
    @Transactional
    public void markAsAvailable(Long carId) {
        var car = carRules.getByIdOrThrow(carId);
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);
    }

    // ------------------------------------------------------------------ //
    //  Image enrichment helpers                                           //
    // ------------------------------------------------------------------ //

    /**
     * Tek araç için — getById / update sonrası.
     * 1 ekstra query ile görselleri yükler, displayOrder'a göre sıralıdır.
     */
    private void enrichWithImages(CarResponse response) {
        List<CarImage> images = carImageRepository.findByCarIdOrderByDisplayOrderAsc(response.getId());
        List<CarImageResponse> imageResponses = carImageMapper.toResponseList(images);
        response.setImages(imageResponses);
        response.setPrimaryImageUrl(findPrimaryUrl(imageResponses));
    }

    /**
     * Çoklu araç için batch fetch — getAll / getAvailable / pagination.
     * N araç için toplam 1 query çalışır (N+1 önlenir).
     *
     * Boş liste → hiçbir query atma.
     */
    private void enrichListWithImages(List<CarResponse> responses) {
        if (responses == null || responses.isEmpty()) {
            return;
        }

        List<Long> carIds = responses.stream().map(CarResponse::getId).toList();
        List<CarImage> allImages = carImageRepository.findAllByCarIdIn(carIds);

        // car.id FK kolonudur — lazy proxy üzerinden ek query atılmaz
        Map<Long, List<CarImage>> imagesByCarId = allImages.stream()
                .collect(Collectors.groupingBy(img -> img.getCar().getId()));

        for (CarResponse response : responses) {
            List<CarImage> carImages = imagesByCarId.getOrDefault(response.getId(), Collections.emptyList());
            List<CarImageResponse> imageResponses = carImageMapper.toResponseList(carImages);
            response.setImages(imageResponses);
            response.setPrimaryImageUrl(findPrimaryUrl(imageResponses));
        }
    }

    /**
     * Liste içindeki kapak görselinin URL'ini döner — yoksa null.
     * CarImageMapper URL'i zaten StorageService üzerinden türetmiş oluyor.
     */
    private String findPrimaryUrl(List<CarImageResponse> images) {
        return images.stream()
                .filter(CarImageResponse::isPrimary)
                .map(CarImageResponse::getUrl)
                .findFirst()
                .orElse(null);
    }
}
