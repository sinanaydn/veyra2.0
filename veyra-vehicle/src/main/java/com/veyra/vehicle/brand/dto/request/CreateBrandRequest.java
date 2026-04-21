package com.veyra.vehicle.brand.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBrandRequest {

    @Schema(description = "Marka adı (benzersiz)", example = "Toyota",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = 2, maxLength = 50)
    @NotBlank(message = "Marka adı boş bırakılamaz")
    @Size(min = 2, max = 50, message = "Marka adı 2-50 karakter arasında olmalıdır")
    private String name;
}
