package com.veyra.vehicle.brand.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBrandRequest {

    @NotBlank(message = "Marka adı boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Marka adı 2-50 karakter arasında olmalıdır")
    private String name;
}