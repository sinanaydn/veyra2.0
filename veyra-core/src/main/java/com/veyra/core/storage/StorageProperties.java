package com.veyra.core.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml → storage.s3.* bind'ı.
 * Record — immutable, auto-validated at startup.
 *
 * @param endpoint         S3 API endpoint (MinIO: http://localhost:9000, R2: https://<acc>.r2.cloudflarestorage.com)
 * @param region           Bölge — R2 için "auto", MinIO için herhangi bir değer olabilir
 * @param bucket           Bucket adı
 * @param accessKey        Access key (MinIO ROOT_USER veya R2 access key ID)
 * @param secretKey        Secret key
 * @param publicBaseUrl    Tarayıcıdan erişilen public base URL — response üretirken kullanılır
 * @param pathStyleAccess  MinIO için true zorunlu, R2 de destekler — virtual-hosted-style yerine path-style
 * @param autoCreateBucket Dev'de true → startup'ta bucket yoksa oluştur. Prod'da false → dashboard'dan oluştur
 */
@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        String endpoint,
        String region,
        String bucket,
        String accessKey,
        String secretKey,
        String publicBaseUrl,
        boolean pathStyleAccess,
        boolean autoCreateBucket
) {
}
