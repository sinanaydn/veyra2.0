package com.veyra.core.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

/**
 * Storage altyapı konfigürasyonu.
 *
 * - {@link StorageProperties}'i aktive eder (@ConfigurationProperties binding)
 * - {@link S3Client} bean'i oluşturur — MinIO, R2 ve AWS S3 ile aynı kod çalışır
 * - Startup'ta bucket'ı kontrol eder, yoksa oluşturur (dev kolaylığı)
 *
 * Prod (R2) için {@code storage.s3.auto-create-bucket=false} yapılır;
 * bucket Cloudflare dashboard'dan oluşturulur.
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class S3StorageConfig {

    private final StorageProperties properties;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
                properties.accessKey(),
                properties.secretKey()
        );

        S3Configuration s3Config = S3Configuration.builder()
                .pathStyleAccessEnabled(properties.pathStyleAccess())
                .build();

        return S3Client.builder()
                .endpointOverride(URI.create(properties.endpoint()))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Config)
                .forcePathStyle(properties.pathStyleAccess())
                .build();
    }

    /**
     * Uygulama tamamen hazır olduğunda bucket'ı kontrol et, yoksa oluştur.
     *
     * {@code auto-create-bucket=false} ise hiçbir şey yapılmaz — prod'da
     * bucket elle oluşturulmuş olmalı, yoksa ilk upload'da fail eder.
     *
     * Bucket kontrolü başarısız olsa bile app çöker değil — sadece log'lanır.
     * Upload sırasında gerçek hata StorageException olarak fırlatılır.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void bootstrapBucket() {
        if (!properties.autoCreateBucket()) {
            log.info("Storage: auto-create-bucket kapalı, bucket bootstrap atlanıyor (bucket={})", properties.bucket());
            return;
        }

        S3Client client = s3Client();
        String bucket = properties.bucket();

        try {
            client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("Storage: bucket '{}' zaten mevcut", bucket);
        } catch (NoSuchBucketException e) {
            try {
                client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                log.info("Storage: bucket '{}' oluşturuldu", bucket);
            } catch (Exception createEx) {
                log.warn("Storage: bucket '{}' oluşturulamadı — ilk upload'da hata verebilir: {}",
                        bucket, createEx.getMessage());
            }
        } catch (Exception e) {
            log.warn("Storage: bucket '{}' kontrolü başarısız — endpoint erişilemiyor olabilir: {}",
                    bucket, e.getMessage());
        }
    }
}
