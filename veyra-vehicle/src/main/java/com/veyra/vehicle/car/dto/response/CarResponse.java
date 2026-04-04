package com.veyra.vehicle.car.dto.response;

import com.veyra.vehicle.car.enums.CarStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {

    private Long id;
    private Long modelId;
    private String modelName;
    private Long brandId;
    private String brandName;
    private int year;
    private int doors;
    private int baggages;
    private BigDecimal dailyPrice;
    private CarStatus status;
    private LocalDateTime createdAt;
}