package com.veyra.vehicle.brand.service;

import com.veyra.vehicle.brand.dto.request.CreateBrandRequest;
import com.veyra.vehicle.brand.dto.request.UpdateBrandRequest;
import com.veyra.vehicle.brand.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {

    BrandResponse create(CreateBrandRequest request);

    BrandResponse update(Long id, UpdateBrandRequest request);

    BrandResponse getById(Long id);

    List<BrandResponse> getAll();

    void delete(Long id);
}
