package com.veyra.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private long   expiresIn;  // ms cinsinden (frontend için)
    private String role;
    private Long   userId;
    private String email;
}
