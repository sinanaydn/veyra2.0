package com.veyra.vehicle.model.mapper;

import com.veyra.vehicle.model.dto.response.CarModelResponse;
import com.veyra.vehicle.model.entity.CarModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarModelMapper {

    // brand.id → brandId, brand.name → brandName
    @Mapping(source = "brand.id", target = "brandId")
    @Mapping(source = "brand.name", target = "brandName")
    CarModelResponse toResponse(CarModel carModel);
}
