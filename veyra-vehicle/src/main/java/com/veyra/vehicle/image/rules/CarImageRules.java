package com.veyra.vehicle.image.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.BusinessRuleException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.vehicle.image.entity.CarImage;
import com.veyra.vehicle.image.repository.CarImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/**
 * CarImage iş kuralları.
 *
 * - Manager asla doğrudan repository.findById().orElseThrow() yazmaz — her şey burada.
 * - Dosya validasyonu burada ortak: boş dosya / yanlış MIME / aşırı boyut / magic bytes.
 * - Ownership kontrolü: bir araca ait olmayan görsel üzerinde işlem yapılamaz.
 *
 * Magic byte doğrulama neden önemli?
 *   Browser'ın gönderdiği {@code Content-Type} header'ı kullanıcı tarafından manipüle
 *   edilebilir. Sadece header whitelist'i yetersizdir. Gerçek dosya formatını
 *   doğrulamak için ilk birkaç byte'ı okuyup sihirli imzayı kontrol ediyoruz.
 */
@Component
@RequiredArgsConstructor
public class CarImageRules {

    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final int  MAX_IMAGES_PER_CAR  = 10;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final CarImageRepository carImageRepository;

    // ------------------------------------------------------------------ //
    //  Fetch + ownership                                                  //
    // ------------------------------------------------------------------ //

    public CarImage getByIdOrThrow(Long id) {
        return carImageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_IMAGE_NOT_FOUND, "Görsel bulunamadı: " + id));
    }

    /**
     * Görseli fetch ederken aynı zamanda aracın sahibi olup olmadığını kontrol eder.
     * Başka araca ait görsel id'si gönderilirse 422 döner — bilgi sızıntısı yok.
     */
    public CarImage getByIdForCarOrThrow(Long imageId, Long carId) {
        return carImageRepository.findByIdAndCarId(imageId, carId)
                .orElseThrow(() -> new BusinessRuleException(
                        ErrorCodes.IMAGE_NOT_OWNED_BY_CAR,
                        "Bu görsel belirtilen araca ait değil"));
    }

    // ------------------------------------------------------------------ //
    //  Dosya validasyonu                                                 //
    // ------------------------------------------------------------------ //

    public void checkFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException(
                    ErrorCodes.FILE_EMPTY, "Yüklenen dosya boş");
        }
    }

    public void checkFileSize(MultipartFile file) {
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException(
                    ErrorCodes.FILE_SIZE_EXCEEDED,
                    "Dosya boyutu 5MB sınırını aşıyor: " + file.getSize() + " byte");
        }
    }

    /**
     * Hem {@code Content-Type} header'ını hem de magic byte'ları kontrol eder.
     * İki kontrol aynı anda geçmelidir — sadece birinin geçmesi yetmez.
     */
    public void checkContentType(MultipartFile file) {
        String declaredType = file.getContentType();
        if (declaredType == null || !ALLOWED_CONTENT_TYPES.contains(declaredType.toLowerCase(Locale.ROOT))) {
            throw new BusinessRuleException(
                    ErrorCodes.FILE_TYPE_INVALID,
                    "Desteklenmeyen dosya tipi. İzin verilen: JPEG, PNG, WebP");
        }

        if (!hasValidImageMagicBytes(file)) {
            throw new BusinessRuleException(
                    ErrorCodes.FILE_TYPE_INVALID,
                    "Dosya içeriği geçerli bir görsel değil (magic byte doğrulaması başarısız)");
        }
    }

    public void checkMaxImagesPerCar(Long carId) {
        long count = carImageRepository.countByCarId(carId);
        if (count >= MAX_IMAGES_PER_CAR) {
            throw new BusinessRuleException(
                    ErrorCodes.CAR_IMAGE_LIMIT_EXCEEDED,
                    "Bir araca en fazla " + MAX_IMAGES_PER_CAR + " görsel yüklenebilir");
        }
    }

    // ------------------------------------------------------------------ //
    //  Magic byte doğrulama                                              //
    // ------------------------------------------------------------------ //

    /**
     * İlk 12 byte'ı okur ve JPEG / PNG / WebP imzalarından birine uyup uymadığına bakar.
     *
     * JPEG:  FF D8 FF
     * PNG:   89 50 4E 47 0D 0A 1A 0A
     * WebP:  "RIFF" ?? ?? ?? ?? "WEBP"  (4-7 arası boyut, sonrası marker)
     */
    private boolean hasValidImageMagicBytes(MultipartFile file) {
        try {
            byte[] header;
            try (var in = file.getInputStream()) {
                // readNBytes: stream segmentli olsa bile tam 12 byte okumayı garanti eder.
                // InputStream.read(byte[]) kısa okuma yapabilir; readNBytes bloklayarak bekler.
                header = in.readNBytes(12);
            }
            if (header.length < 12) return false;

            // JPEG
            if ((header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF) {
                return true;
            }

            // PNG
            if ((header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && (header[4] & 0xFF) == 0x0D
                    && (header[5] & 0xFF) == 0x0A
                    && (header[6] & 0xFF) == 0x1A
                    && (header[7] & 0xFF) == 0x0A) {
                return true;
            }

            // WebP: "RIFF" + 4 byte size + "WEBP"
            return (header[0] & 0xFF) == 'R' && (header[1] & 0xFF) == 'I'
                    && (header[2] & 0xFF) == 'F' && (header[3] & 0xFF) == 'F'
                    && (header[8] & 0xFF) == 'W' && (header[9] & 0xFF) == 'E'
                    && (header[10] & 0xFF) == 'B' && (header[11] & 0xFF) == 'P';
        } catch (IOException e) {
            return false;
        }
    }
}
