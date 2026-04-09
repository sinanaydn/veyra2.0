package com.veyra.core.util;

import org.springframework.security.core.Authentication;

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
}
