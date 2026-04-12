---
name: veyra-app
description: Spring Boot entry point modülü. VeyraApplication main class, SwaggerConfig, application.yml. Tüm modülleri aggregate eder ve fat JAR olarak paketlenir.
---

## Sorumluluk
Spring Boot uygulamasının başlatıldığı modül. Diğer modüllerin tamamına bağımlıdır ve Spring Boot Maven plugin ile fat JAR olarak paketlenir. Çalıştırılabilir tek modül.

## VeyraApplication

```java
@SpringBootApplication(scanBasePackages = "com.veyra")
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.veyra")
@EntityScan(basePackages = "com.veyra")
@EnableScheduling
@EnableCaching
```

- `scanBasePackages = "com.veyra"` — diğer modüllerdeki `@Component`, `@Service`, `@RestController` taranır
- `@EnableJpaAuditing` — `BaseEntity.createdAt` / `updatedAt` otomatik dolar
- `@EnableScheduling` — `RefreshTokenCleanupTask`, `RateLimitFilter.cleanup()` gibi scheduled task'lar
- `@EnableCaching` — `@Cacheable` / `@CacheEvict` aktif (Brand, CarModel listeleri)

## SwaggerConfig
SpringDoc OpenAPI ayarları:
- API title, version, description
- JWT bearer auth scheme tanımı (`Authorization` header)
- Tüm endpoint'lerde JWT token gönderilebilmesi için global security requirement
- `SWAGGER_ENABLED` env var ile kontrol edilir (prod'da `false`)

## application.yml — Önemli Bölümler

### Server & Performance
```yaml
spring:
  threads:
    virtual:
      enabled: true            # Virtual Threads (Java 25)
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
  lifecycle:
    timeout-per-shutdown-phase: 30s

server:
  port: 8080
  shutdown: graceful           # Aktif istekler tamamlanana kadar bekler
  compression:
    enabled: true
    min-response-size: 1024
    mime-types: application/json,application/xml,text/html,text/xml,text/plain
  tomcat:
    max-http-form-post-size: 1MB
    max-swallow-size: 1MB
```

### Datasource (PostgreSQL + HikariCP)
```yaml
spring:
  datasource:
    url: ${SPRING_DATASOURCE_URL}
    username: ${SPRING_DATASOURCE_USERNAME}
    password: ${SPRING_DATASOURCE_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 10000
      idle-timeout: 300000
      max-lifetime: 600000
  jpa:
    hibernate:
      ddl-auto: ${DDL_AUTO:update}   # dev: update, prod: validate
    open-in-view: false
    show-sql: false
```

### JWT
```yaml
jwt:
  secret: ${JWT_SECRET}              # Zorunlu, fallback yok
  expiration: 900000                 # 15 dakika (ms)
  refresh-expiration-days: 7         # 7 gün
```

### Swagger (prod'da kapatılabilir)
```yaml
springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:true}
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:true}
```

### Admin Seed
```yaml
admin:
  email: ${ADMIN_EMAIL:admin@veyra.com}
  password: ${ADMIN_PASSWORD}        # Zorunlu, fallback yok
```

### CORS
```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000,http://localhost:5173}
```

### Storage (S3-compatible — MinIO dev / Cloudflare R2 prod)
```yaml
storage:
  s3:
    endpoint: ${STORAGE_S3_ENDPOINT}           # MinIO: http://localhost:9000, R2: https://<acc>.r2.cloudflarestorage.com
    region: ${STORAGE_S3_REGION:us-east-1}
    bucket: ${STORAGE_S3_BUCKET:veyra-car-images}
    access-key: ${STORAGE_S3_ACCESS_KEY}
    secret-key: ${STORAGE_S3_SECRET_KEY}
    public-base-url: ${STORAGE_S3_PUBLIC_BASE_URL}    # CDN/public base URL — runtime'da URL türetmek için
    path-style: ${STORAGE_S3_PATH_STYLE:true}          # MinIO zorunlu, R2 opsiyonel
    auto-create-bucket: ${STORAGE_S3_AUTO_CREATE_BUCKET:true}  # Dev için startup'ta bucket oluşturur
```
`veyra-core`'daki `StorageProperties` bu bloğu `@ConfigurationProperties` ile bind eder. `S3StorageConfig` bucket bootstrap'i `ApplicationReadyEvent` üzerinde yapar.

## pom.xml
- Parent: `veyra-api` (multi-module root)
- Tüm modüller dependency olarak listelenir
- `spring-boot-starter-cache` — Spring Cache
- `spring-boot-starter-actuator` — health endpoint
- `springdoc-openapi-starter-webmvc-ui` — Swagger
- `spring-boot-maven-plugin` `repackage` goal'üyle fat JAR üretir

## Docker
- `Dockerfile` — multi-stage build, `eclipse-temurin:25-jre`
- `docker-compose.yml` — PostgreSQL + **MinIO** + app, `env_file: .env` ile secret yönetimi
  - `minio` service: ports `9000` (S3 API) + `9001` (console), `minio_data` volume, healthcheck
  - `veyra-app` hem `postgres` hem `minio` health'ına bağımlı (`depends_on.condition: service_healthy`)
- `.env.example` — tüm env var'ların şablonu
- **Dev başlatma:** `docker-compose up -d postgres minio` → MinIO console `http://localhost:9001`

## Bağımlılıklar
- **Tüm modüller** — core, auth, user, vehicle, rental, payment
