package com.veyra.vehicle.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarModelResponse {

    private Long id;
    private String name;
    private Long brandId;
    private String brandName;
    private LocalDateTime createdAt;
}
