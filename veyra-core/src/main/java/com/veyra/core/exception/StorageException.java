package com.veyra.core.exception;

/**
 * Object storage (MinIO / Cloudflare R2 / S3) altyapı hataları için fırlatılır.
 *
 * Bu istisna kullanıcıdan kaynaklanan bir hata değildir — yanlış dosya tipi,
 * boş dosya gibi client tarafı hataları {@link BusinessRuleException} ile 422 döner.
 * StorageException yalnızca storage katmanının kendisinin başarısız olduğu
 * durumlar içindir: bucket erişilemez, upload başarısız, network hatası vb.
 *
 * HTTP 500 Internal Server Error döndürür.
 */
public class StorageException extends BusinessException {

    public StorageException(String errorCode, String message) {
        super(errorCode, message, 500);
    }

    public StorageException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, 500);
        initCause(cause);
    }
}
