package com.veyra.vehicle.car.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCarRequest {

    @NotNull(message = "Model ID boş bırakılamaz")
    private Long modelId;

    @Min(value = 2000, message = "Yıl 2000'den küçük olamaz")
    private int year;

    @Min(value = 2, message = "Kapı sayısı en az 2 olmalıdır")
    private int doors;

    @Min(value = 0, message = "Bagaj sayısı negatif olamaz")
    private int baggages;

    @NotNull(message = "Günlük fiyat boş bırakılamaz")
    @Positive(message = "Günlük fiyat pozitif olmalıdır")
    private BigDecimal dailyPrice;
}