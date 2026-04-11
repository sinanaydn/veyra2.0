package com.veyra.vehicle.image.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import com.veyra.vehicle.image.dto.request.ReorderImagesRequest;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import com.veyra.vehicle.image.service.CarImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

/**
 * Car image endpoint'leri.
 *
 * Path şeması nested:
 *   /api/v1/cars/{carId}/images[/{imageId}[/primary]]
 *
 * Yetki kuralları:
 *  - GET  → authenticated (her giriş yapmış kullanıcı)
 *  - POST, PUT, DELETE → ADMIN
 *
 * DELETE zaten SecurityConfig'deki {@code .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")}
 * kuralıyla otomatik ADMIN'e kısıtlı; yine de @PreAuthorize eklenmedi, çift kontrol olmasın.
 */
@RestController
@RequestMapping(ApiConstants.CARS + "/{carId}/images")
@RequiredArgsConstructor
public class CarImageController {

    private final CarImageService carImageService;

    /**
     * Yeni görsel yükler — multipart/form-data, field adı: {@code file}.
     * İlk görsel otomatik primary olur.
     */
    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarImageResponse>> upload(
            @PathVariable Long carId,
            @RequestPart("file") MultipartFile file) {
        CarImageResponse response = carImageService.upload(carId, file);
        return ResponseEntity.status(201).body(ApiResponse.created(response));
    }

    /**
     * Bir araca ait tüm görselleri displayOrder'a göre döndürür.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CarImageResponse>>> getByCarId(@PathVariable Long carId) {
        return ResponseEntity.ok(ApiResponse.success(carImageService.getByCarId(carId)));
    }

    /**
     * Görseli kapak (primary) yapar.
     */
    @PutMapping("/{imageId}/primary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarImageResponse>> setPrimary(
            @PathVariable Long carId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResponse.success(carImageService.setPrimary(carId, imageId)));
    }

    /**
     * Araca ait görsellerin sıralamasını toplu günceller.
     * Body: {@code { "items": [ { "imageId": 1, "displayOrder": 3 }, ... ] }}
     */
    @PutMapping("/reorder")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CarImageResponse>>> reorder(
            @PathVariable Long carId,
            @Valid @RequestBody ReorderImagesRequest request) {
        return ResponseEntity.ok(ApiResponse.success(carImageService.reorder(carId, request)));
    }

    /**
     * Görseli siler — storage'dan anında, DB'den soft delete.
     */
    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long carId,
            @PathVariable Long imageId) {
        carImageService.delete(carId, imageId);
        return ResponseEntity.noContent().build();
    }
}
