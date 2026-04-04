package com.veyra.auth.token;

import com.veyra.auth.role.Role;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * JWT işlemleri sözleşmesi.
 * SecurityConfig ve JwtAuthenticationFilter bu interface'e bağlıdır — DIP.
 */
public interface JwtService {

    String generateToken(UserDetails userDetails, Long userId, Role role);

    String extractEmail(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
}
