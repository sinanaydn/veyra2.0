package com.veyra.vehicle.car.dto.request;

import com.veyra.vehicle.car.enums.FuelType;
import com.veyra.vehicle.car.enums.TransmissionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
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

    @NotNull(message = "Yakıt tipi boş bırakılamaz")
    private FuelType fuelType;

    @NotNull(message = "Vites tipi boş bırakılamaz")
    private TransmissionType transmission;

    @Min(value = 1, message = "Koltuk sayısı en az 1 olmalıdır")
    private int seats;

    @NotBlank(message = "Renk boş bırakılamaz")
    @Size(max = 50, message = "Renk en fazla 50 karakter olabilir")
    private String color;

    @Min(value = 0, message = "Kilometre negatif olamaz")
    private int mileage;

    @Size(max = 1000, message = "Açıklama en fazla 1000 karakter olabilir")
    private String description;
}