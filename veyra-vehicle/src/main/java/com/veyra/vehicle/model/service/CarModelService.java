package com.veyra.vehicle.model.service;

import com.veyra.vehicle.model.dto.request.CreateCarModelRequest;
import com.veyra.vehicle.model.dto.request.UpdateCarModelRequest;
import com.veyra.vehicle.model.dto.response.CarModelResponse;

import java.util.List;

public interface CarModelService {

    CarModelResponse create(CreateCarModelRequest request);

    CarModelResponse update(Long id, UpdateCarModelRequest request);

    CarModelResponse getById(Long id);

    List<CarModelResponse> getAll();

    List<CarModelResponse> getAllByBrandId(Long brandId);

    void delete(Long id);
}
