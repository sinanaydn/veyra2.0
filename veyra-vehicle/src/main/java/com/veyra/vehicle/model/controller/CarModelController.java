package com.veyra.vehicle.model.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import com.veyra.vehicle.model.dto.request.CreateCarModelRequest;
import com.veyra.vehicle.model.dto.request.UpdateCarModelRequest;
import com.veyra.vehicle.model.dto.response.CarModelResponse;
import com.veyra.vehicle.model.service.CarModelService;
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

@RestController
@RequestMapping(ApiConstants.CAR_MODELS)
@RequiredArgsConstructor
public class CarModelController {

    private final CarModelService carModelService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarModelResponse>> create(@Valid @RequestBody CreateCarModelRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(carModelService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarModelResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarModelRequest request) {
        return ResponseEntity.ok(ApiResponse.success(carModelService.update(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarModelResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(carModelService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CarModelResponse>>> getAll(
            @RequestParam(required = false) Long brandId) {
        var result = brandId != null
                ? carModelService.getAllByBrandId(brandId)
                : carModelService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carModelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
