package com.veyra.vehicle.car.mapper;

import com.veyra.vehicle.car.dto.response.CarResponse;
import com.veyra.vehicle.car.entity.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarMapper {

    @Mapping(source = "model.id",        target = "modelId")
    @Mapping(source = "model.name",      target = "modelName")
    @Mapping(source = "model.brand.id",  target = "brandId")
    @Mapping(source = "model.brand.name",target = "brandName")
    @Mapping(target = "images",          ignore = true)
    @Mapping(target = "primaryImageUrl", ignore = true)
    CarResponse toResponse(Car car);
}