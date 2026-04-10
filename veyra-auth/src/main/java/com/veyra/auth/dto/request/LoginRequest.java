package com.veyra.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "E-posta boş bırakılamaz")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olmalıdır")
    private String email;

    @NotBlank(message = "Şifre boş bırakılamaz")
    @Size(max = 128, message = "Şifre en fazla 128 karakter olmalıdır")
    private String password;
}
