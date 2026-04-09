package com.veyra.auth.token;

import com.veyra.auth.role.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * JJWT 0.12.x API'sı kullanılarak JWT üretir ve doğrular.
 *
 * Token içeriği (claims):
 *   sub      → e-posta (Spring Security username)
 *   userId   → veyra-user.User.id
 *   role     → ADMIN veya USER
 *   iat/exp  → üretim/son kullanma tarihi
 */
@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService {

    private static final int MIN_SECRET_BYTES = 32; // HS256 için minimum 256 bit

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET env var set edilmemiş. Uygulama başlatılamaz.");
        }
        if (secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET en az 32 byte (256 bit) olmalıdır. Mevcut uzunluk: "
                    + secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length + " byte.");
        }
    }

    @Override
    public String generateToken(UserDetails userDetails, Long userId, Role role) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userId)
                .claim("role", role.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    // ------------------------------------------------------------------ //
    //  Private yardımcı metotlar
    // ------------------------------------------------------------------ //

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
