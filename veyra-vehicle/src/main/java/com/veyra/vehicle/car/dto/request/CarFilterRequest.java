package com.veyra.vehicle.car.dto.request;

import com.veyra.vehicle.car.enums.FuelType;
import com.veyra.vehicle.car.enums.TransmissionType;

import java.math.BigDecimal;

public record CarFilterRequest(
        Long brandId,
        Long modelId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minYear,
        Integer maxYear,
        FuelType fuelType,
        TransmissionType transmission,
        Boolean available
) {}
