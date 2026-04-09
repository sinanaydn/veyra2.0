package com.veyra.core.event;

/**
 * User soft-delete edildiğinde yayınlanır.
 * Dinleyiciler: veyra-auth (AuthUser soft-delete + refresh token revoke).
 */
public record UserDeletedEvent(Long userId, String email) {}
