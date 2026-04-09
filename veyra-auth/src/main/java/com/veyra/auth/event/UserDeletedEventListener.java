package com.veyra.auth.event;

import com.veyra.auth.token.RefreshTokenService;
import com.veyra.auth.user.repository.AuthUserRepository;
import com.veyra.core.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * User soft-delete edildiğinde AuthUser'ı da soft-delete eder ve refresh token'ları revoke eder.
 * Spring Event ile gevşek bağlantı — veyra-user → veyra-auth bağımlılığı oluşturmaz.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final AuthUserRepository  authUserRepository;
    private final RefreshTokenService refreshTokenService;

    @EventListener
    @Transactional
    public void onUserDeleted(UserDeletedEvent event) {
        authUserRepository.findByEmail(event.email()).ifPresent(authUser -> {
            authUser.setDeleted(true);
            authUserRepository.save(authUser);

            refreshTokenService.revokeAllByAuthUserId(authUser.getId());

            log.info("AuthUser soft-deleted and tokens revoked for userId={}, email={}",
                    event.userId(), event.email());
        });
    }
}
