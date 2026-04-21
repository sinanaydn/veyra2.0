---
name: veyra-core
description: Tüm modüllerin kullandığı temel altyapı — BaseEntity, ApiResult, exception hierarchy, ErrorCodes, GlobalExceptionHandler, ApiConstants. Hiçbir modüle bağımlı değildir.
---

## Sorumluluk
Çekirdek altyapı modülü. Diğer tüm modüller buna bağımlıdır, ancak bu modül hiçbir şeye bağımlı değildir. Ortak DTO'lar, exception'lar, sabitler ve audit tabanlı entity sınıfı burada tutulur.

## BaseEntity
Tüm JPA entity'leri `BaseEntity` extend eder.

| Alan | Tip | Açıklama |
|------|-----|---------|
| `id` | `Long` | `@GeneratedValue` |
| `createdAt` | `LocalDateTime` | `@CreatedDate`, audit ile dolar |
| `updatedAt` | `LocalDateTime` | `@LastModifiedDate` |
| `deleted` | `boolean` | Soft delete flag, default `false` |

`@EnableJpaAuditing` `veyra-app` içinde aktif.

## ApiResult<T>
Tüm endpoint'lerin döndürdüğü zarf. Constructor private, sadece factory metotlar.

> **İsimlendirme notu:** Swagger'ın `io.swagger.v3.oas.annotations.responses.ApiResponse` annotation'ı ile çakışmayı önlemek için `ApiResult` adı tercih edildi. Bu class runtime HTTP body envelope'u; Swagger'ınki compile-time doc annotation'ı — ikisi farklı katmanlardır.

| Factory | HTTP | Kullanım |
|---------|------|---------|
| `success(data)` | 200 | Standart başarılı GET/PUT |
| `success(data, message)` | 200 | Custom mesajla |
| `created(data)` | 201 | POST sonrası |
| `noContent()` | 204 | DELETE sonrası |
| `error(message, errorCode, status)` | * | GlobalExceptionHandler kullanır |
| `validationError(fieldErrors)` | 400 | Validation hatası |

JSON şekli:
```json
{ "success": true, "status": 200, "message": "...", "data": {...}, "errorCode": null, "timestamp": "..." }
```

`@JsonInclude(NON_NULL)` — null alanlar JSON'a yazılmaz.

## PageResponse<T>
Pagination için generic wrapper. Tüm listeleme endpoint'lerinde aktif kullanılır (`cars`, `rentals`, `payments`, `users`). `Page<T>` alır, frontend-dostu düz yapıya dönüştürür.

## UserDeletedEvent
`com.veyra.core.event.UserDeletedEvent` — cross-module kullanıcı silme cascade'i için Spring Application Event.
```java
public record UserDeletedEvent(Long userId, String email) {}
```
`veyra-user` tarafından yayınlanır, `veyra-auth` tarafından dinlenir. AuthUser soft-delete ve refresh token revoke işlemleri bu event üzerinden tetiklenir.

## Exception Hierarchy

```
BusinessException (abstract)
├── ResourceNotFoundException  → 404
├── AlreadyExistsException     → 409
├── UnauthorizedException      → 401
├── ForbiddenException         → 403
└── BusinessRuleException      → 422
```

Her exception `errorCode` ve `message` alır. `GlobalExceptionHandler` HTTP status'a map eder.

## GlobalExceptionHandler

| Yakaladığı | Status | Notlar |
|------------|--------|--------|
| `ResourceNotFoundException` | 404 | |
| `AlreadyExistsException` | 409 | |
| `UnauthorizedException` | 401 | |
| `ForbiddenException` | 403 | |
| `BusinessRuleException` | 422 | İş kuralı ihlali |
| `MethodArgumentNotValidException` | 400 | Field hatalarını `Map<String,String>`'e çevirir |
| `ConstraintViolationException` | 400 | Query param validation hataları |
| `HttpMessageNotReadableException` | 400 | Bozuk JSON — internal bilgi sızdırmaz |
| `AccessDeniedException` | 403 | Spring Security |
| `Exception` | 500 | Generic mesaj döner, stack trace log'lanır |

Controller ve Manager'da **try-catch yok** — tüm hatalar buraya gelir.

## ErrorCodes
String sabitler. Frontend bu kodları kullanarak çok dilli mesaj gösterebilir.

Kategoriler:
- **Auth:** `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `ACCESS_DENIED`, `AUTH_USER_NOT_FOUND`, `ROLE_ALREADY_ASSIGNED`
- **User:** `USER_NOT_FOUND`, `EMAIL_ALREADY_EXISTS`
- **Brand:** `BRAND_NOT_FOUND`, `BRAND_ALREADY_EXISTS`
- **CarModel:** `CAR_MODEL_NOT_FOUND`, `CAR_MODEL_ALREADY_EXISTS`
- **Car:** `CAR_NOT_FOUND`, `CAR_NOT_AVAILABLE`
- **Rental:** `RENTAL_NOT_FOUND`, `RENTAL_ALREADY_ACTIVE`, `RENTAL_NOT_ACTIVE`, `RENTAL_DATE_INVALID`
- **Payment:** `PAYMENT_NOT_FOUND`, `PAYMENT_ALREADY_DONE`
- **Rate Limit:** `RATE_LIMIT_EXCEEDED`
- **Genel:** `VALIDATION_ERROR`, `INTERNAL_ERROR`

## ApiConstants
Tüm API path öneklerini merkezi tutar:
```java
API_V1 = "/api/v1"
AUTH, USERS, BRANDS, CAR_MODELS, CARS, RENTALS, PAYMENTS, ADMIN
```

## Storage Abstraction (S3-compatible)

`com.veyra.core.storage` paketi — dev'de MinIO, prod'da Cloudflare R2 (her ikisi de S3 API uyumlu). Tek implementasyon, sadece env var'lar değişir.

### StorageService interface
| Metot | Davranış |
|-------|---------|
| `upload(MultipartFile, folder)` | `{folder}/{uuid}.{ext}` key üretir, S3'e yazar, `StoredFile` döner |
| `delete(storageKey)` | İdempotent — obje yoksa hata vermez |
| `getPublicUrl(storageKey)` | `{publicBaseUrl}/{storageKey}` — CDN/vendor değişse bile DB etkilenmez |

### StoredFile record
```java
public record StoredFile(String storageKey, String contentType, long sizeBytes) {}
```
**URL YOK** — bilinçli karar. Public URL formatı vendor/CDN değişiminde bozulabilir, signed URL'e geçiş mümkün olmalı. Bu yüzden DB'de sadece `storageKey` saklanır, URL çağrı anında türetilir.

### StorageProperties
`@ConfigurationProperties("storage.s3")` record. 8 alan: `endpoint`, `region`, `bucket`, `accessKey`, `secretKey`, `publicBaseUrl`, `pathStyle`, `autoCreateBucket`.

### S3StorageConfig
- `S3Client` bean — `pathStyleAccess=true`, `forcePathStyle=true` (MinIO için zorunlu)
- `@EventListener(ApplicationReadyEvent.class) bootstrapBucket()` — `autoCreateBucket=true` ise startup'ta bucket yoksa oluşturur (dev/docker için)

### StorageException
`BusinessException` subclass'ı — HTTP 500. Upload/delete hatalarında kullanılır, S3 kütüphanesi exception'ları sarar.

### Path traversal koruması
`S3StorageService.buildStorageKey()` — orijinal filename kullanmaz. UUID + regex-sanitized extension (`[a-z0-9]{1,10}`). Böylece `../../etc/passwd` gibi saldırılar mümkün değil.

### Bağımlılık
`software.amazon.awssdk:s3` (AWS SDK v2, `veyra-api/pom.xml`'de BOM ile versiyon yönetilir).

## SecurityUtils
`com.veyra.core.util.SecurityUtils` — controller ve manager'larda kullanılan yardımcı metotlar.

| Metot | Davranış |
|-------|---------|
| `isAdmin(Authentication)` | `ROLE_ADMIN` authority var mı kontrol eder |
| `checkOwnership(entityUserId, email, isAdmin, userIdResolver)` | Admin ise atlanır; değilse email'den userId çözümler ve entity sahibiyle karşılaştırır. Eşleşmezse `ForbiddenException` (`ACCESS_DENIED`) |

`checkOwnership` tüm ownership kontrollerini merkezi tutar — `veyra-rental` ve `veyra-payment` bu metodu kullanır.

## Bağımlılıklar
**Hiçbir şey.** Bu modül leaf'tir.
