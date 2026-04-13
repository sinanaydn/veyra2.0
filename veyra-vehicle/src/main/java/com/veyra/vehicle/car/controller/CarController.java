package com.veyra.vehicle.car.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import com.veyra.core.response.PageResponse;
import com.veyra.vehicle.car.dto.request.CarFilterRequest;
import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;
import com.veyra.vehicle.car.service.CarService;
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


@RestController
@RequestMapping(ApiConstants.CARS)
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarResponse>> create(@Valid @RequestBody CreateCarRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(carService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CarResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCarRequest request) {
        return ResponseEntity.ok(ApiResponse.success(carService.update(id, request)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CarResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(carService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<CarResponse>>> getAll(
            CarFilterRequest filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(carService.search(filter, pageable)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        carService.delete(id);
        return ResponseEntity.noContent().build();
    }
}