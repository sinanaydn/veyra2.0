package com.veyra.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Tüm endpoint'lerin döndürdüğü evrensel yanıt zarfı.
 * Frontend her zaman aynı JSON şeklini alır.
 *
 * Dışarıdan new ile oluşturulamaz — static factory metotlar kullanılır (OCP).
 * null değerler JSON'a yazılmaz (@JsonInclude).
 *
 * NOT: @Builder generic sınıflarda static factory metotlarla birlikte
 * tip gölgeleme (type shadowing) uyarısı üretir.
 * Bu yüzden private constructor tercih edildi — daha temiz ve uyarısız.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final String errorCode;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResponse(boolean success, int status, String message, String errorCode, T data) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    // ------------------------------------------------------------------ //
    //  Başarılı yanıtlar
    // ------------------------------------------------------------------ //

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), "İşlem başarılı", null, data);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), message, null, data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), "Kayıt oluşturuldu", null, data);
    }

    public static ApiResponse<Void> noContent() {
        return new ApiResponse<>(true, HttpStatus.NO_CONTENT.value(), "Kayıt silindi", null, null);
    }

    // ------------------------------------------------------------------ //
    //  Hata yanıtları
    // ------------------------------------------------------------------ //

    public static ApiResponse<Void> error(String message, String errorCode, int status) {
        return new ApiResponse<>(false, status, message, errorCode, null);
    }

    public static <T> ApiResponse<T> validationError(T fieldErrors) {
        return new ApiResponse<>(false, HttpStatus.BAD_REQUEST.value(), "Doğrulama başarısız", "VALIDATION_ERROR", fieldErrors);
    }
}
