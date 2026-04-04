package com.veyra.core.exception;

import org.springframework.http.HttpStatus;

/**
 * İstenen kayıt veritabanında bulunamadığında fırlatılır.
 * HTTP 404 Not Found döndürür.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND.value());
    }
}
