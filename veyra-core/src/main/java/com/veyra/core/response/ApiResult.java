package com.veyra.core.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Tüm endpoint'lerin döndürdüğü evrensel yanıt zarfı.
 * Frontend her zaman aynı JSON şeklini alır.
 *
 * <p><b>İsimlendirme:</b> Swagger'ın {@code io.swagger.v3.oas.annotations.responses.ApiResponse}
 * annotation'ı ile çakışmayı önlemek için {@code ApiResult} adı tercih edildi.
 * Bu class → runtime HTTP body envelope'u; Swagger'ınki → compile-time doc annotation'ı.</p>
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
public class ApiResult<T> {

    private final boolean success;
    private final int status;
    private final String message;
    private final String errorCode;
    private final T data;
    private final LocalDateTime timestamp;

    private ApiResult(boolean success, int status, String message, String errorCode, T data) {
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

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(true, HttpStatus.OK.value(), "İşlem başarılı", null, data);
    }

    public static <T> ApiResult<T> success(T data, String message) {
        return new ApiResult<>(true, HttpStatus.OK.value(), message, null, data);
    }

    public static <T> ApiResult<T> created(T data) {
        return new ApiResult<>(true, HttpStatus.CREATED.value(), "Kayıt oluşturuldu", null, data);
    }

    public static ApiResult<Void> noContent() {
        return new ApiResult<>(true, HttpStatus.NO_CONTENT.value(), "Kayıt silindi", null, null);
    }

    // ------------------------------------------------------------------ //
    //  Hata yanıtları
    // ------------------------------------------------------------------ //

    public static ApiResult<Void> error(String message, String errorCode, int status) {
        return new ApiResult<>(false, status, message, errorCode, null);
    }

    public static <T> ApiResult<T> validationError(T fieldErrors) {
        return new ApiResult<>(false, HttpStatus.BAD_REQUEST.value(), "Doğrulama başarısız", "VALIDATION_ERROR", fieldErrors);
    }
}
