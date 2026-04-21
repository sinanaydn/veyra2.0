package com.veyra.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterRequest {

    @Schema(description = "Kullanıcı adı", example = "Ahmet",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    @NotBlank(message = "Ad boş bırakılamaz")
    @Size(max = 50, message = "Ad en fazla 50 karakter olmalıdır")
    private String firstName;

    @Schema(description = "Kullanıcı soyadı", example = "Yılmaz",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 50)
    @NotBlank(message = "Soyad boş bırakılamaz")
    @Size(max = 50, message = "Soyad en fazla 50 karakter olmalıdır")
    private String lastName;

    @Schema(description = "Kullanıcı e-posta adresi", example = "ahmet@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
    @Email(message = "Geçerli bir e-posta adresi giriniz")
    @NotBlank(message = "E-posta boş bırakılamaz")
    @Size(max = 255, message = "E-posta en fazla 255 karakter olmalıdır")
    private String email;

    @Schema(
            description = "Şifre — en az 10 karakter, en az bir küçük harf, bir büyük harf, bir rakam ve bir özel karakter (@#$%^&+=!.,?_-) içermelidir",
            example = "StrongPass1!",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 10, maxLength = 128
    )
    @NotBlank(message = "Şifre boş bırakılamaz")
    @Size(min = 10, max = 128, message = "Şifre 10-128 karakter arasında olmalıdır")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!.,?_-]).+$",
            message = "Şifre en az bir küçük harf, bir büyük harf, bir rakam ve bir özel karakter içermelidir"
    )
    private String password;

    @Schema(description = "Telefon (opsiyonel) — 10-15 karakter, rakam ve + ( ) - boşluk", example = "+905551112233",
            maxLength = 15, nullable = true)
    @Size(max = 15, message = "Telefon numarası en fazla 15 karakter olmalıdır")
    @Pattern(regexp = "^$|^[0-9+()\\-\\s]{10,15}$", message = "Geçerli bir telefon numarası giriniz")
    private String phone;
}
