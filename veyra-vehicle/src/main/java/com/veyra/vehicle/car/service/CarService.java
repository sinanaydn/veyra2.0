package com.veyra.vehicle.car.service;

import com.veyra.core.response.PageResponse;
import com.veyra.vehicle.car.dto.request.CarFilterRequest;
import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;
import org.springframework.data.domain.Pageable;

public interface CarService {

    CarResponse create(CreateCarRequest request);

    CarResponse update(Long id, UpdateCarRequest request);

    CarResponse getById(Long id);

    PageResponse<CarResponse> getAll(Pageable pageable);

    PageResponse<CarResponse> search(CarFilterRequest filter, Pageable pageable);

    void delete(Long id);

    void markAsRented(Long carId);

    void markAsAvailable(Long carId);
}