package com.veyra.core.exception;

import lombok.Getter;

/**
 * Tüm domain istisnalarının türediği temel sınıf.
 * Abstract — direkt new ile oluşturulamaz, her zaman alt sınıf kullanılır (LSP).
 *
 * errorCode  → makine okunabilir hata kodu (frontend switch-case için)
 * httpStatus → GlobalExceptionHandler'a HTTP statüsünü bildirir
 */
@Getter
public abstract class BusinessException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    protected BusinessException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
