package com.veyra.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Kimlik doğrulama başarısız olduğunda veya token geçersiz/süresi dolmuş olduğunda fırlatılır.
 * HTTP 401 Unauthorized döndürür.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.UNAUTHORIZED.value());
    }
}
