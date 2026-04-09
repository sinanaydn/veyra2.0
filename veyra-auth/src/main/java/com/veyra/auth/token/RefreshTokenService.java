package com.veyra.auth.token;

/**
 * Refresh token yaşam döngüsü sözleşmesi.
 */
public interface RefreshTokenService {

    /** Yeni refresh token üretir ve DB'ye kaydeder. */
    RefreshToken create(Long authUserId);

    /**
     * Token'ı doğrular ve rotation uygular:
     * eski token silinir, yeni token üretilip döndürülür.
     * Geçersiz veya süresi dolmuş token için UnauthorizedException fırlatır.
     */
    RefreshToken validateAndRotate(String token);

    /** Kullanıcıya ait tüm refresh token'ları siler (logout). */
    void revokeAllByAuthUserId(Long authUserId);

    /**
     * Token string'e göre sahibini bulur ve tüm token'larını siler.
     * Token geçersizse sessizce geçer (idempotent logout).
     */
    void revokeByToken(String token);
}
