package com.veyra.core.constants;

/**
 * Tüm modüllerde kullanılan makine okunabilir hata kodları.
 * Frontend bu kodlara göre kullanıcıya mesaj gösterebilir.
 * final class — instantiate edilemez, sadece sabit tutar (utility class).
 */
public final class ErrorCodes {

    private ErrorCodes() {}

    // Auth
    public static final String INVALID_CREDENTIALS  = "INVALID_CREDENTIALS";
    public static final String TOKEN_EXPIRED         = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID         = "TOKEN_INVALID";
    public static final String ACCESS_DENIED         = "ACCESS_DENIED";

    // User
    public static final String USER_NOT_FOUND        = "USER_NOT_FOUND";
    public static final String EMAIL_ALREADY_EXISTS  = "EMAIL_ALREADY_EXISTS";

    // Brand
    public static final String BRAND_NOT_FOUND       = "BRAND_NOT_FOUND";
    public static final String BRAND_ALREADY_EXISTS  = "BRAND_ALREADY_EXISTS";

    // CarModel
    public static final String CAR_MODEL_NOT_FOUND      = "CAR_MODEL_NOT_FOUND";
    public static final String CAR_MODEL_ALREADY_EXISTS = "CAR_MODEL_ALREADY_EXISTS";

    // Car
    public static final String CAR_NOT_FOUND         = "CAR_NOT_FOUND";
    public static final String CAR_NOT_AVAILABLE     = "CAR_NOT_AVAILABLE";

    // Rental
    public static final String RENTAL_NOT_FOUND      = "RENTAL_NOT_FOUND";
    public static final String RENTAL_ALREADY_ACTIVE = "RENTAL_ALREADY_ACTIVE";
    public static final String RENTAL_NOT_ACTIVE     = "RENTAL_NOT_ACTIVE";
    public static final String RENTAL_DATE_INVALID   = "RENTAL_DATE_INVALID";

    // Payment
    public static final String PAYMENT_NOT_FOUND     = "PAYMENT_NOT_FOUND";
    public static final String PAYMENT_ALREADY_DONE  = "PAYMENT_ALREADY_DONE";

    // Genel
    public static final String VALIDATION_ERROR      = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR        = "INTERNAL_ERROR";
}
