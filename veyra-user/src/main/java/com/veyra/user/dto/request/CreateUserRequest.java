package com.veyra.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Ad boş bırakılamaz")
    @Size(max = 50, message = "Ad en fazla 50 karakter olmalıdır")
    private String firstName;

    @NotBlank(message = "Soyad boş bırakılamaz")
    @Size(max = 50, message = "Soyad en fazla 50 karakter olmalıdır")
    private String lastName;

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "E-posta boş bırakılamaz")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olmalıdır")
    private String email;

    @Size(max = 15, message = "Telefon numarası en fazla 15 karakter olmalıdır")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{10,15}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;
}
