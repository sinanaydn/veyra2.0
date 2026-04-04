package com.veyra.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Kullanıcı kimliği doğrulanmış fakat bu işlemi yapmaya yetkisi yoksa fırlatılır.
 * HTTP 403 Forbidden döndürür.
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.FORBIDDEN.value());
    }
}
