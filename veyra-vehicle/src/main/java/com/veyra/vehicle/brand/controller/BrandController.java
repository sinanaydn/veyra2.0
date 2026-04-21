package com.veyra.vehicle.brand.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.veyra.vehicle.brand.dto.request.CreateBrandRequest;
import com.veyra.vehicle.brand.dto.request.UpdateBrandRequest;
import com.veyra.vehicle.brand.dto.response.BrandResponse;
import com.veyra.vehicle.brand.service.BrandService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Brand endpoint'leri. GET'ler public, yazma işlemleri ADMIN.
 * Ortak hata yanıtları {@code OpenApiOperationCustomizer} tarafından eklenir.
 */
@Tag(name = "Brands")
@RestController
@RequestMapping(ApiConstants.BRANDS)
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @Operation(
            summary = "Marka oluştur",
            description = "Yeni araç markası ekler. **Yetki:** ROLE_ADMIN.",
            responses = @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<BrandResponse>> create(@Valid @RequestBody CreateBrandRequest request) {
        return ResponseEntity.status(201).body(ApiResult.created(brandService.create(request)));
    }

    @Operation(
            summary = "Marka güncelle",
            description = "Marka adını günceller. **Yetki:** ROLE_ADMIN.",
            responses = @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<BrandResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(ApiResult.success(brandService.update(id, request)));
    }

    @Operation(summary = "Marka detayı", description = "**Public endpoint.**")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<BrandResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(brandService.getById(id)));
    }

    @Operation(
            summary = "Tüm markaları listele",
            description = "Cache'li. **Public endpoint** — Rate limit: 60 istek / 60 sn."
    )
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResult<List<BrandResponse>>> getAll() {
        return ResponseEntity.ok(ApiResult.success(brandService.getAll()));
    }

    @Operation(summary = "Marka sil", description = "Soft delete. **Yetki:** ROLE_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        brandService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
