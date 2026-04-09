package com.veyra.core.util;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.ForbiddenException;
import org.springframework.security.core.Authentication;

import java.util.function.Function;

/**
 * Spring Security Authentication nesnesi üzerinde ortak yardımcı metotlar.
 * Controller'larda tekrar eden rol kontrolü mantığını tek yerde toplar.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Kaynak sahipliğini kontrol eder. Admin ise atlanır, değilse userId karşılaştırması yapılır.
     *
     * @param entityUserId   Kaynağın sahibinin userId'si
     * @param email          Mevcut kullanıcının email'i (JWT'den)
     * @param isAdmin        Mevcut kullanıcı admin mi
     * @param userIdResolver Email'den userId çözümleyen fonksiyon (örn. userRules::getUserIdByEmail)
     */
    public static void checkOwnership(Long entityUserId, String email, boolean isAdmin,
                                       Function<String, Long> userIdResolver) {
        if (!isAdmin) {
            Long currentUserId = userIdResolver.apply(email);
            if (!entityUserId.equals(currentUserId)) {
                throw new ForbiddenException(ErrorCodes.ACCESS_DENIED, "Bu kayda erişim yetkiniz yok");
            }
        }
    }
}
