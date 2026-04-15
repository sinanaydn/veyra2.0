package com.veyra.auth.user.entity;

import com.veyra.auth.role.Role;
import com.veyra.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * Kimlik doğrulama bilgilerini tutar: e-posta, şifre (hash), rol.
 *
 * Profil bilgileri (ad, soyad, telefon) veyra-user modülündedir — SRP.
 * userId: veyra-user.User tablosuna referans (FK değil — modüller arası bağımlılığı gevşetir).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auth_users")
@SQLRestriction("deleted = false")
public class AuthUser extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // veyra-user.User tablosuna soft reference — @ManyToOne değil (modüler mimari)
    @Column(nullable = false, unique = true)
    private Long userId;

    // --- Hesap kilitleme (brute-force koruması) ---

    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private LocalDateTime lockedUntil;

    public boolean isAccountLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void recordFailedLogin(int maxAttempts, long lockMinutes) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
        }
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
