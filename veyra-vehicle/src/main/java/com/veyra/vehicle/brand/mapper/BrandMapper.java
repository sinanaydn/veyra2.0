package com.veyra.vehicle.brand.mapper;

import com.veyra.vehicle.brand.dto.request.CreateBrandRequest;
import com.veyra.vehicle.brand.dto.response.BrandResponse;
import com.veyra.vehicle.brand.entity.Brand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    Brand toEntity(CreateBrandRequest request);

    BrandResponse toResponse(Brand brand);
}
