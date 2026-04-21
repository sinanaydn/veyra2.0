package com.veyra.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequest {

    @Schema(description = "Login veya refresh sonucu alınan refresh token", example = "eyJhbGciOiJIUzI1NiJ9...",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Refresh token boş bırakılamaz")
    private String refreshToken;
}
