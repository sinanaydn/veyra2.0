package com.veyra.auth.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Süresi dolmuş refresh token'ları periyodik olarak temizler.
 * Her 6 saatte bir çalışır — DB bloat'ını engeller.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // 6 saat
    @Transactional
    public void purgeExpiredTokens() {
        int deleted = refreshTokenRepository.deleteAllExpired(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Expired refresh token temizlendi: {} adet", deleted);
        }
    }
}
