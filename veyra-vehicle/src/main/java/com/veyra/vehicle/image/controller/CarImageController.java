package com.veyra.vehicle.image.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import com.veyra.vehicle.image.dto.request.ReorderImagesRequest;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import com.veyra.vehicle.image.service.CarImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Car image endpoint'leri.
 *
 * Path şeması nested: {@code /api/v1/cars/{carId}/images[/{imageId}[/primary]]}
 *
 * Yetki kuralları:
 *  - GET  → public
 *  - POST, PUT, DELETE → ADMIN
 *
 * DELETE, SecurityConfig'deki {@code .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")}
 * kuralıyla ADMIN'e kısıtlı; method-level @PreAuthorize eklenmedi.
 */
@Tag(name = "Car Images")
@RestController
@RequestMapping(ApiConstants.CARS + "/{carId}/images")
@RequiredArgsConstructor
public class CarImageController {

    private final CarImageService carImageService;

    @Operation(
            summary = "Araç görseli yükle",
            description = """
                    Çoklu parça upload (multipart/form-data). Field adı: `file`.

                    **Kısıtlar:**
                    - Maksimum dosya boyutu: **5 MB**
                    - İzin verilen MIME tipler: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`
                    - Magic byte doğrulaması yapılır (uzantı sahteciliğine karşı)
                    - Araç başına maksimum **10 görsel**
                    - İlk yüklenen görsel otomatik `primary` (kapak) olur

                    **Yetki:** ROLE_ADMIN.
                    """
    )
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarImageResponse>> upload(
            @PathVariable Long carId,
            @RequestPart("file")
            @Parameter(
                    description = "Yüklenecek görsel dosyası (max 5 MB, jpeg/png/webp)",
                    content = @Content(mediaType = "multipart/form-data",
                            schema = @Schema(type = "string", format = "binary"))
            )
            MultipartFile file) {
        CarImageResponse response = carImageService.upload(carId, file);
        return ResponseEntity.status(201).body(ApiResult.created(response));
    }

    @Operation(
            summary = "Araç görsellerini listele",
            description = """
                    Belirtilen araca ait tüm görselleri `displayOrder` sırasına göre döndürür.
                    **Public endpoint** — Rate limit: 60 istek / 60 sn.
                    """
    )
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResult<List<CarImageResponse>>> getByCarId(@PathVariable Long carId) {
        return ResponseEntity.ok(ApiResult.success(carImageService.getByCarId(carId)));
    }

    @Operation(
            summary = "Görseli kapak (primary) yap",
            description = "Belirtilen görseli aracın kapak görseli yapar; diğer görsellerin `primary` flag'ini false'a çeker. **Yetki:** ROLE_ADMIN."
    )
    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarImageResponse>> setPrimary(
            @PathVariable Long carId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResult.success(carImageService.setPrimary(carId, imageId)));
    }

    @Operation(
            summary = "Görselleri yeniden sırala",
            description = """
                    Araca ait görsellerin `displayOrder` değerlerini toplu günceller.

                    Body örneği:
                    ```json
                    { "items": [ { "imageId": 1, "displayOrder": 3 }, { "imageId": 2, "displayOrder": 1 } ] }
                    ```

                    **Yetki:** ROLE_ADMIN.
                    """
    )
    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<List<CarImageResponse>>> reorder(
            @PathVariable Long carId,
            @Valid @RequestBody ReorderImagesRequest request) {
        return ResponseEntity.ok(ApiResult.success(carImageService.reorder(carId, request)));
    }

    @Operation(summary = "Görseli sil", description = "Storage'dan anında, DB'den soft delete. **Yetki:** ROLE_ADMIN.")
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long carId,
            @PathVariable Long imageId) {
        carImageService.delete(carId, imageId);
        return ResponseEntity.noContent().build();
    }
}
