package com.veyra.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @NotBlank(message = "Ad boş bırakılamaz")
    @Size(max = 50, message = "Ad en fazla 50 karakter olmalıdır")
    private String firstName;

    @NotBlank(message = "Soyad boş bırakılamaz")
    @Size(max = 50, message = "Soyad en fazla 50 karakter olmalıdır")
    private String lastName;

    @Size(max = 15, message = "Telefon numarası en fazla 15 karakter olmalıdır")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{10,15}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;
}
