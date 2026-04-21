package com.veyra.vehicle.car.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import com.veyra.core.response.PageResponse;
import com.veyra.vehicle.car.dto.request.CarFilterRequest;
import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;
import com.veyra.vehicle.car.service.CarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "Cars")
@RestController
@RequestMapping(ApiConstants.CARS)
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @Operation(
            summary = "Araç oluştur",
            description = "Yeni araç ekler. **Yetki:** ROLE_ADMIN."
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarResponse>> create(@Valid @RequestBody CreateCarRequest request) {
        return ResponseEntity.status(201).body(ApiResult.created(carService.create(request)));
    }

    @Operation(
            summary = "Araç güncelle",
            description = "Araç bilgilerini günceller (durum, fiyat, vb). **Yetki:** ROLE_ADMIN."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarRequest request) {
        return ResponseEntity.ok(ApiResult.success(carService.update(id, request)));
    }

    @Operation(
            summary = "Araç detayı",
            description = "Aracı ID ile getirir (görseller dahil). **Public endpoint.**"
    )
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<CarResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(carService.getById(id)));
    }

    @Operation(
            summary = "Araç katalog + filtreleme",
            description = """
                    Araçları filtreler ve sayfalar.

                    **Filter query string (flat)** — nested object değil:
                    `?brandId=1&modelId=5&minPrice=100&maxPrice=500&minYear=2020&maxYear=2024&fuelType=GASOLINE&transmission=AUTOMATIC&available=true`

                    **Pagination query:** `?page=0&size=20&sort=dailyPrice,asc`
                    (`sort` parametresi birden fazla kez gönderilebilir)

                    Default: `page=0, size=20`.

                    **Public endpoint** — Rate limit: 60 istek / 60 sn.
                    """
    )
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResult<PageResponse<CarResponse>>> getAll(
            CarFilterRequest filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.success(carService.search(filter, pageable)));
    }

    @Operation(summary = "Araç sil", description = "Soft delete. **Yetki:** ROLE_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
