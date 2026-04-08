---
name: veyra-core
description: Tüm modüllerin kullandığı temel altyapı — BaseEntity, ApiResponse, exception hierarchy, ErrorCodes, GlobalExceptionHandler, ApiConstants. Hiçbir modüle bağımlı değildir.
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

## ApiResponse<T>
Tüm endpoint'lerin döndürdüğü zarf. Constructor private, sadece factory metotlar.

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
Pagination için generic wrapper. Şu an aktif kullanılmıyor; pagination eklenince devreye girer.

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
| `AccessDeniedException` | 403 | Spring Security |
| `Exception` | 500 | Generic mesaj döner, stack trace log'lanır |

Controller ve Manager'da **try-catch yok** — tüm hatalar buraya gelir.

## ErrorCodes
String sabitler. Frontend bu kodları kullanarak çok dilli mesaj gösterebilir.

Kategoriler:
- **Auth:** `INVALID_CREDENTIALS`, `TOKEN_EXPIRED`, `TOKEN_INVALID`, `ACCESS_DENIED`
- **User:** `USER_NOT_FOUND`, `EMAIL_ALREADY_EXISTS`
- **Brand:** `BRAND_NOT_FOUND`, `BRAND_ALREADY_EXISTS`
- **CarModel:** `CAR_MODEL_NOT_FOUND`, `CAR_MODEL_ALREADY_EXISTS`
- **Car:** `CAR_NOT_FOUND`, `CAR_NOT_AVAILABLE`
- **Rental:** `RENTAL_NOT_FOUND`, `RENTAL_ALREADY_ACTIVE`, `RENTAL_NOT_ACTIVE`, `RENTAL_DATE_INVALID`
- **Payment:** `PAYMENT_NOT_FOUND`, `PAYMENT_ALREADY_DONE`
- **Genel:** `VALIDATION_ERROR`, `INTERNAL_ERROR`

## ApiConstants
Tüm API path öneklerini merkezi tutar:
```java
API_V1 = "/api/v1"
AUTH, USERS, BRANDS, CAR_MODELS, CARS, RENTALS, PAYMENTS
```

## Bağımlılıklar
**Hiçbir şey.** Bu modül leaf'tir.
