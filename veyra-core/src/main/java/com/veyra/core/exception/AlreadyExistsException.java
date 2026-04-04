package com.veyra.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Kayıt zaten mevcut olduğunda fırlatılır (ör: aynı isimde marka, aynı e-posta).
 * HTTP 409 Conflict döndürür.
 */
public class AlreadyExistsException extends BusinessException {

    public AlreadyExistsException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT.value());
    }
}
