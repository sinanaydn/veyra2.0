package com.veyra.core.handler;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.*;
import com.veyra.core.response.ApiResult;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tüm modüllerdeki exception'ları merkezi olarak yakalar ve ApiResult formatına dönüştürür.
 * Controller'lar try-catch yazmaz — SRP.
 *
 * İşleme önceliği (yukarıdan aşağıya):
 *  1. Spesifik business exception'lar
 *  2. Bean Validation hataları
 *  3. Tüm diğer exception'lar (500 fallback)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ //
    //  Business exception'lar — her biri kendi HTTP statusunu taşır
    // ------------------------------------------------------------------ //

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResult<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    @ExceptionHandler(AlreadyExistsException.class)
    public ResponseEntity<ApiResult<Void>> handleConflict(AlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResult<Void>> handleUnauthorized(UnauthorizedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResult<Void>> handleForbidden(ForbiddenException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessRule(BusinessRuleException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResult<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResult.error("Bu işlem için yetkiniz yok", ErrorCodes.ACCESS_DENIED, HttpStatus.FORBIDDEN.value()));
    }

    // BusinessException'ın diğer alt sınıfları için genel yakalayıcı
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ApiResult.error(ex.getMessage(), ex.getErrorCode(), ex.getHttpStatus()));
    }

    // ------------------------------------------------------------------ //
    //  @Valid / @Validated — Bean Validation hataları
    // ------------------------------------------------------------------ //

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {

        // Merge function eklendi: aynı field'a birden fazla hata gelirse birleştirilir
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "Geçersiz değer",
                        (existing, duplicate) -> existing + ", " + duplicate
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.validationError(fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResult<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(message, ErrorCodes.VALIDATION_ERROR, HttpStatus.BAD_REQUEST.value()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResult<Void>> handleMalformedJson(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(
                        "İstek gövdesi okunamadı — JSON formatını kontrol edin",
                        ErrorCodes.VALIDATION_ERROR,
                        HttpStatus.BAD_REQUEST.value()
                ));
    }

    // ------------------------------------------------------------------ //
    //  500 — Beklenmedik hatalar için fallback
    // ------------------------------------------------------------------ //

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleGeneral(Exception ex) {
        log.error("Beklenmedik hata oluştu: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(
                        "Sunucu hatası oluştu, lütfen daha sonra tekrar deneyin",
                        ErrorCodes.INTERNAL_ERROR,
                        HttpStatus.INTERNAL_SERVER_ERROR.value()
                ));
    }
}
