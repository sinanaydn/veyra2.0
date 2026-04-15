package com.veyra.auth.rules;

import com.veyra.auth.token.RefreshToken;
import com.veyra.auth.token.RefreshTokenRepository;
import com.veyra.auth.user.entity.AuthUser;
import com.veyra.auth.user.repository.AuthUserRepository;
import com.veyra.core.constants.ErrorCodes;
import com.veyra.auth.role.Role;
import com.veyra.core.exception.AlreadyExistsException;
import com.veyra.core.exception.BusinessRuleException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Auth iş kuralları — AuthManager'ı temiz tutar (SRP).
 * Her kural ihlali anında ilgili exception fırlatır.
 */
@Component
@RequiredArgsConstructor
public class AuthRules {

    private static final int  MAX_FAILED_ATTEMPTS    = 5;
    private static final long LOCK_DURATION_MINUTES   = 15;

    private final AuthUserRepository     authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder        passwordEncoder;

    public void checkIfEmailAlreadyExists(String email) {
        if (authUserRepository.existsByEmail(email)) {
            throw new AlreadyExistsException(
                    ErrorCodes.EMAIL_ALREADY_EXISTS,
                    "Bu e-posta adresi zaten kayıtlı"
            );
        }
    }

    public RefreshToken getRefreshTokenOrThrow(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCodes.TOKEN_INVALID, "Geçersiz refresh token"));
    }

    public AuthUser getByEmailOrThrow(String email) {
        return authUserRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCodes.INVALID_CREDENTIALS, "E-posta veya şifre hatalı"));
    }

    public AuthUser getByIdOrThrow(Long id) {
        return authUserRepository.findById(id)
                .orElseThrow(() -> new UnauthorizedException(
                        ErrorCodes.TOKEN_INVALID, "Refresh token'a ait kullanıcı bulunamadı"));
    }

    public AuthUser getByUserIdOrThrow(Long userId) {
        return authUserRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.AUTH_USER_NOT_FOUND,
                        "Kullanıcıya ait kimlik kaydı bulunamadı: " + userId));
    }

    public void checkIfRoleAlreadyAssigned(AuthUser authUser, Role role) {
        if (authUser.getRole() == role) {
            throw new BusinessRuleException(
                    ErrorCodes.ROLE_ALREADY_ASSIGNED,
                    "Kullanıcı zaten " + role.name() + " rolüne sahip");
        }
    }

    // ------------------------------------------------------------------ //
    //  Hesap kilitleme & şifre doğrulama kuralları
    // ------------------------------------------------------------------ //

    /**
     * Hesap kilitli mi kontrol eder. Kilitliyse UnauthorizedException fırlatır.
     */
    public void checkIfAccountLocked(AuthUser authUser) {
        if (authUser.isAccountLocked()) {
            throw new UnauthorizedException(
                    ErrorCodes.ACCOUNT_LOCKED,
                    "Hesap çok fazla başarısız giriş denemesi nedeniyle geçici olarak kilitlendi. " +
                    "Lütfen " + LOCK_DURATION_MINUTES + " dakika sonra tekrar deneyin.");
        }
    }

    /**
     * Şifreyi doğrular. Yanlışsa başarısız giriş sayacını artırır,
     * eşik aşılırsa hesabı kilitler ve UnauthorizedException fırlatır.
     */
    public void validatePasswordOrRecordFailure(AuthUser authUser, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, authUser.getPasswordHash())) {
            authUser.recordFailedLogin(MAX_FAILED_ATTEMPTS, LOCK_DURATION_MINUTES);
            authUserRepository.save(authUser);
            throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS, "E-posta veya şifre hatalı");
        }
    }

    /**
     * Başarılı giriş sonrası başarısız deneme sayacını sıfırlar.
     * Sayaç zaten sıfırsa gereksiz DB yazımı yapmaz.
     */
    public void resetFailedAttemptsIfNeeded(AuthUser authUser) {
        if (authUser.getFailedLoginAttempts() > 0) {
            authUser.resetFailedLoginAttempts();
            authUserRepository.save(authUser);
        }
    }
}
