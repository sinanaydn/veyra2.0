package com.veyra.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {

    @Schema(description = "Kullanıcı e-posta adresi", example = "user@veyra.com",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "E-posta boş bırakılamaz")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olmalıdır")
    private String email;

    @Schema(description = "Hesap şifresi", example = "MyPassw0rd!",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128)
    @NotBlank(message = "Şifre boş bırakılamaz")
    @Size(max = 128, message = "Şifre en fazla 128 karakter olmalıdır")
    private String password;
}
