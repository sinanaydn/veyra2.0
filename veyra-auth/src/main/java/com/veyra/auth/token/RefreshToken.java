package com.veyra.auth.token;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DB'de saklanan refresh token kaydı.
 * BaseEntity'den extend etmez — soft delete gereksiz, fiziksel silme tercih edilir.
 * Token rotation uygulanır: her /refresh isteğinde eski token silinir, yeni üretilir.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // AuthUser tablosuna soft reference
    @Column(nullable = false)
    private Long authUserId;

    @Column(nullable = false)
    private LocalDateTime expiresAt;
}
