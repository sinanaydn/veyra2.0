package com.veyra.vehicle.car.service;

import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;

import java.util.List;

public interface CarService {

    CarResponse create(CreateCarRequest request);

    CarResponse update(Long id, UpdateCarRequest request);

    CarResponse getById(Long id);

    List<CarResponse> getAll();

    List<CarResponse> getAvailable();

    void delete(Long id);
}