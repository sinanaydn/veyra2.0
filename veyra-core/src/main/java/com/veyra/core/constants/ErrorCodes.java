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

    // Auth User
    public static final String AUTH_USER_NOT_FOUND  = "AUTH_USER_NOT_FOUND";
    public static final String ROLE_ALREADY_ASSIGNED = "ROLE_ALREADY_ASSIGNED";

    // Rate Limit
    public static final String RATE_LIMIT_EXCEEDED   = "RATE_LIMIT_EXCEEDED";

    // Storage (object storage — MinIO/R2/S3)
    public static final String FILE_UPLOAD_FAILED    = "FILE_UPLOAD_FAILED";
    public static final String FILE_DELETE_FAILED    = "FILE_DELETE_FAILED";
    public static final String STORAGE_UNAVAILABLE   = "STORAGE_UNAVAILABLE";

    // File validation (client-side — 422)
    public static final String FILE_EMPTY            = "FILE_EMPTY";
    public static final String FILE_TYPE_INVALID     = "FILE_TYPE_INVALID";
    public static final String FILE_SIZE_EXCEEDED    = "FILE_SIZE_EXCEEDED";

    // Car Image
    public static final String CAR_IMAGE_NOT_FOUND       = "CAR_IMAGE_NOT_FOUND";
    public static final String CAR_IMAGE_LIMIT_EXCEEDED  = "CAR_IMAGE_LIMIT_EXCEEDED";
    public static final String IMAGE_NOT_OWNED_BY_CAR    = "IMAGE_NOT_OWNED_BY_CAR";

    // Genel
    public static final String VALIDATION_ERROR      = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR        = "INTERNAL_ERROR";
}
