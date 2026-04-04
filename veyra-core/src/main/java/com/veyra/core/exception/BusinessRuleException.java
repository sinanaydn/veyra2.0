package com.veyra.core.exception;

/**
 * İş kuralı ihlallerinde fırlatılır.
 * "Zaten var" değil — kuralı karşılamayan bir işlem söz konusu.
 * Örnek: araç müsait değil, kiralama tarihleri çakışıyor.
 * HTTP 422 Unprocessable Entity döndürür.
 */
public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(String errorCode, String message) {
        super(errorCode, message, 422);
    }
}