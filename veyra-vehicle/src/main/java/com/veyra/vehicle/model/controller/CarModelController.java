package com.veyra.vehicle.model.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.veyra.vehicle.model.dto.request.CreateCarModelRequest;
import com.veyra.vehicle.model.dto.request.UpdateCarModelRequest;
import com.veyra.vehicle.model.dto.response.CarModelResponse;
import com.veyra.vehicle.model.service.CarModelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Car Models")
@RestController
@RequestMapping(ApiConstants.CAR_MODELS)
@RequiredArgsConstructor
public class CarModelController {

    private final CarModelService carModelService;

    @Operation(
            summary = "Model oluştur",
            description = "Belirtilen markaya yeni model ekler. **Yetki:** ROLE_ADMIN.",
            responses = @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    )
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarModelResponse>> create(@Valid @RequestBody CreateCarModelRequest request) {
        return ResponseEntity.status(201).body(ApiResult.created(carModelService.create(request)));
    }

    @Operation(
            summary = "Model güncelle",
            description = "Model adını ve marka bağlantısını günceller. **Yetki:** ROLE_ADMIN.",
            responses = @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<CarModelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarModelRequest request) {
        return ResponseEntity.ok(ApiResult.success(carModelService.update(id, request)));
    }

    @Operation(summary = "Model detayı", description = "**Public endpoint.**")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<CarModelResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(carModelService.getById(id)));
    }

    @Operation(
            summary = "Model listesi",
            description = """
                    `brandId` query param'ı ile filtrelenebilir. Cache'li.
                    **Public endpoint** — Rate limit: 60 istek / 60 sn.
                    """
    )
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResult<List<CarModelResponse>>> getAll(
            @Parameter(description = "Filtre — sadece bu markanın modelleri", example = "1")
            @RequestParam(required = false) Long brandId) {
        var result = brandId != null
                ? carModelService.getAllByBrandId(brandId)
                : carModelService.getAll();
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @Operation(summary = "Model sil", description = "Soft delete. **Yetki:** ROLE_ADMIN.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
