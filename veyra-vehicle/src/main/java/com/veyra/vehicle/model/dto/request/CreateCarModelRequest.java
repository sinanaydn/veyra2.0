package com.veyra.vehicle.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCarModelRequest {

    @NotBlank(message = "Model adı boş bırakılamaz")
    @Size(min = 1, max = 50, message = "Model adı 1-50 karakter arasında olmalıdır")
    private String name;

    @NotNull(message = "Marka ID boş bırakılamaz")
    private Long brandId;
}
