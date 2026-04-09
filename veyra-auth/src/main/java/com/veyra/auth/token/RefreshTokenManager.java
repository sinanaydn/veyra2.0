package com.veyra.auth.token;

import com.veyra.auth.rules.AuthRules;
import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenManager implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthRules              authRules;

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Override
    @Transactional
    public RefreshToken create(Long authUserId) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .authUserId(authUserId)
                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays))
                .build();

        return refreshTokenRepository.save(token);
    }

    @Override
    @Transactional
    public RefreshToken validateAndRotate(String token) {
        RefreshToken existing = authRules.getRefreshTokenOrThrow(token);

        if (existing.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(existing);
            throw new UnauthorizedException(
                    ErrorCodes.TOKEN_EXPIRED, "Refresh token süresi dolmuş, yeniden giriş yapın");
        }

        // Token rotation — eski token silinir, yeni üretilir
        refreshTokenRepository.delete(existing);
        return create(existing.getAuthUserId());
    }

    @Override
    @Transactional
    public void revokeAllByAuthUserId(Long authUserId) {
        refreshTokenRepository.deleteByAuthUserId(authUserId);
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        refreshTokenRepository.findByToken(token)
                .ifPresent(rt -> refreshTokenRepository.deleteByAuthUserId(rt.getAuthUserId()));
    }
}
