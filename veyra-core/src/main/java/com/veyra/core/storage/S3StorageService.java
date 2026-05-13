package com.veyra.core.storage;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.StorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * S3 API uyumlu servislerle (MinIO, Cloudflare R2, AWS S3) çalışan tek implementation.
 * Endpoint + credentials değişince aynı kod farklı vendor'da çalışır.
 *
 * Önemli tasarım kararları:
 *  - storageKey random UUID ile üretilir → dosya adından bağımsız, path traversal kapanır
 *  - Orijinal dosya uzantısı korunur (browser content-type negotiation için)
 *  - Public URL DB'de tutulmaz, {@link #getPublicUrl(String)} her çağrıda taze
 *    presigned GET URL üretir → bucket PRIVATE kalır, güvenli + ölçeklenebilir
 *  - Delete idempotent — var olmayan key için hata fırlatmaz (S3 zaten idempotent)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final StorageProperties properties;

    @Override
    public StoredFile upload(MultipartFile file, String folder) {
        String storageKey = buildStorageKey(folder, file.getOriginalFilename());
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            log.debug("Storage: upload OK — key={}, size={}B, type={}",
                    storageKey, file.getSize(), contentType);

            return new StoredFile(storageKey, contentType, file.getSize());

        } catch (IOException e) {
            throw new StorageException(
                    ErrorCodes.FILE_UPLOAD_FAILED,
                    "Dosya okunamadı: " + e.getMessage(),
                    e
            );
        } catch (SdkException e) {
            throw new StorageException(
                    ErrorCodes.FILE_UPLOAD_FAILED,
                    "Storage servisine yükleme başarısız: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(request);

            log.debug("Storage: delete OK — key={}", storageKey);

        } catch (SdkException e) {
            throw new StorageException(
                    ErrorCodes.FILE_DELETE_FAILED,
                    "Storage'dan silme başarısız: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Private bucket'tan geçici imzalı GET URL üretir.
     *
     * Bucket PRIVATE olduğu için tarayıcı direkt erişemez — bu URL,
     * S3 v4 signature ile imzalanmış query string içerir ve TTL süresince geçerlidir.
     * TTL dolduğunda URL otomatik invalid olur (güvenlik kazancı).
     *
     * URL her response'ta yeniden üretilir; DB'de tutulmaz.
     * CDN/vendor/TTL değişimi DB migration gerektirmez.
     */
    @Override
    public String getPublicUrl(String storageKey) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(storageKey)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(properties.presignedUrlTtlMinutes()))
                    .getObjectRequest(getRequest)
                    .build();

            PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
            return presigned.url().toString();

        } catch (SdkException e) {
            throw new StorageException(
                    ErrorCodes.FILE_UPLOAD_FAILED,
                    "Presigned URL üretilemedi: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * Rastgele + çakışmasız storage key üretir.
     * Format: {folder}/{uuid}.{ext}
     *
     * Örnek: "cars/42/a1b2c3d4-....jpg"
     *
     * Orijinal dosya adı KULLANILMAZ:
     *  - Path traversal güvenlik riski yok
     *  - Aynı isimli iki dosya birbirini ezmez
     *  - Türkçe/özel karakter sorunları yaşanmaz
     */
    private String buildStorageKey(String folder, String originalFilename) {
        String extension = extractExtension(originalFilename);
        String uuid = UUID.randomUUID().toString();
        String cleanFolder = (folder == null || folder.isBlank()) ? "misc" : folder.replaceAll("^/+|/+$", "");
        return cleanFolder + "/" + uuid + (extension.isEmpty() ? "" : "." + extension);
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) return "";
        String ext = filename.substring(dotIndex + 1).toLowerCase();
        // Sadece alfanumerik uzantılara izin ver — güvenlik
        return ext.matches("[a-z0-9]{1,10}") ? ext : "";
    }
}
