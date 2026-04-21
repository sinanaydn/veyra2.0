# CLAUDE.md

Bu dosya proje **geneline** ait bilgileri içerir. Modüle özgü detaylar için ilgili modülün içindeki `SKILL.md` dosyasına bakın.

---

## Build & Run

Tüm Maven komutları `veyra-api/` dizininden çalıştırılır.

```bash
mvn clean install -DskipTests           # Tüm projeyi build et
mvn -pl veyra-app spring-boot:run       # Uygulamayı lokalde çalıştır
docker-compose up -d                    # PostgreSQL + MinIO'yu Docker'da başlat
```

- Server: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- MinIO console: `http://localhost:9001` (dev object storage)

---

## Modüller

| Modül | Sorumluluk | Detay |
|-------|------------|-------|
| `veyra-core` | BaseEntity, ApiResult, exceptions, ErrorCodes | `veyra-core/SKILL.md` |
| `veyra-auth` | JWT auth, SecurityConfig, AdminSeeder | `veyra-auth/SKILL.md` |
| `veyra-user` | User profile CRUD | `veyra-user/SKILL.md` |
| `veyra-vehicle` | brand + model + car | `veyra-vehicle/SKILL.md` |
| `veyra-rental` | Kiralama akışı | `veyra-rental/SKILL.md` |
| `veyra-payment` | Ödeme simülasyonu | `veyra-payment/SKILL.md` |
| `veyra-app` | Spring Boot entry point, config | `veyra-app/SKILL.md` |

**Bağımlılık yönü (tek yönlü):**
```
veyra-app     → tüm modüller
veyra-payment → veyra-core, veyra-rental, veyra-vehicle, veyra-user
veyra-rental  → veyra-core, veyra-vehicle, veyra-user
veyra-vehicle → veyra-core                 (StorageService + AWS SDK v2 buradan gelir)
veyra-user    → veyra-core
veyra-auth    → veyra-core, veyra-user
veyra-core    → AWS SDK v2 (software.amazon.awssdk:s3)
```

---

## Paket Düzeni (her domain modülü)

```
com.veyra.<module>.<domain>/
├── entity/       # JPA entities — @SQLRestriction("deleted = false") zorunlu
├── repository/   # Spring Data JPA
├── service/      # Interface — controller buna bağımlı
├── manager/      # Service implementation
├── rules/        # İş kuralı validasyonu — getByIdOrThrow pattern
├── controller/   # REST endpoint'ler
├── dto/request/  # Validation annotations'lı DTO'lar
├── dto/response/
├── mapper/       # MapStruct
└── enums/
```

---

## Evrensel Kurallar (tüm modüllerde geçerli)

### Rules pattern — `getByIdOrThrow`
Her Rules sınıfı varlık kontrolü + fetch'i tek metotta yapar. Manager **asla** `repository.findById().orElseThrow()` yazmaz; Rules'a devreder.

### Soft delete
`deleted = true` + `save()` — asla `deleteById()`. Her entity'de `@SQLRestriction("deleted = false")` zorunlu.

### Update uniqueness
Update'te `existsByXxxAndIdNot(...)` kullanılır — kendi kaydı false positive vermesin.

### Exception hierarchy
```
BusinessException (abstract)
├── ResourceNotFoundException  → 404
├── AlreadyExistsException     → 409
├── UnauthorizedException      → 401
├── ForbiddenException         → 403
└── BusinessRuleException      → 422
```
`GlobalExceptionHandler` hepsini yakalar. Controller/Manager'da try-catch yok.

### `@Transactional`
- Yazma metotları: `@Transactional`
- Okuma metotları: `@Transactional(readOnly = true)`

### `ApiResult<T>` envelope
Tüm endpoint'ler `ApiResult<T>` döndürür (Swagger'ın `@ApiResponse` annotation'ı ile isim çakışmasını önlemek için `ApiResult` adı seçildi):
```json
{ "success": true, "status": 200, "message": "...", "data": {...}, "errorCode": null, "timestamp": "..." }
```

---

## API Base Paths

| Path | Modül |
|------|-------|
| `/api/v1/auth/**` | veyra-auth |
| `/api/v1/admin/**` | veyra-auth (AdminController) |
| `/api/v1/users/**` | veyra-user |
| `/api/v1/brands/**`, `/models/**`, `/cars/**` | veyra-vehicle |
| `/api/v1/cars/{carId}/images/**` | veyra-vehicle (image sub-domain) |
| `/api/v1/rentals/**` | veyra-rental |
| `/api/v1/payments/**` | veyra-payment |

Endpoint detayları ve yetki kuralları için ilgili modülün `SKILL.md` dosyasına bakın.

---

## Veritabanı & Auth

- **PostgreSQL 17** — Docker, app lokalde
- `ddl-auto: update` (dev) / `validate` (prod)
- **JWT** — stateless; claims: `email`, `userId`, `role`; access 15 dk, refresh 7 gün (DB, revoke edilebilir)
- **AdminSeeder** — ilk başlatmada `ADMIN_EMAIL` / `ADMIN_PASSWORD` env var'larından admin oluşturur; `ADMIN_PASSWORD` zorunludur, set edilmezse startup durur
- **Virtual Threads** — `spring.threads.virtual.enabled: true` (Java 25)
- **Cache** — Brand/CarModel listeleri `@Cacheable`, CUD'da `@CacheEvict`
- **Compression** — Tomcat gzip, 1KB üzeri JSON response'lar sıkıştırılır
- **Object Storage** — S3-compatible abstraction (`veyra-core.StorageService`), dev'de **MinIO** (Docker), prod'da **Cloudflare R2**. DB'de sadece `storageKey` saklanır, public URL runtime'da türetilir (vendor lock-in yok).
- **Env vars** — `CORS_ALLOWED_ORIGINS`, `DDL_AUTO`, `SWAGGER_ENABLED`, `JWT_SECRET`, `ADMIN_PASSWORD` (zorunlu), `STORAGE_S3_*` (endpoint, region, bucket, access-key, secret-key, public-base-url, path-style, auto-create-bucket)

---

## Public Catalog & Rate Limiting

**Public (authentication YOK):**
- `GET /api/v1/cars/**` (araç detay + listeleme + görseller)
- `GET /api/v1/brands/**`
- `GET /api/v1/models/**`

Rent-a-car ziyaretçileri giriş yapmadan katalogu gezebilir. Yazma operasyonları (POST/PUT/DELETE) `@PreAuthorize` + SecurityConfig matcher ile **ADMIN**'e kısıtlıdır.

**RateLimitFilter (veyra-auth) — iki bucket:**
| Bucket | Kapsam | Limit |
|--------|--------|-------|
| `AUTH`   | `/api/v1/auth/**` (login/register/refresh) | 5 req / 60 sn |
| `PUBLIC` | GET `/cars\|/brands\|/models/**`          | 60 req / 60 sn |
| `NONE`   | Authenticated endpoint'ler                  | Limit YOK (JWT zaten kimlik doğruluyor) |

Key format: `{ip}:{bucket}` — aynı IP iki bucket'ta ayrı sayılır, biri dolsa diğer etkilenmez. `X-Forwarded-For` sadece `TRUSTED_PROXIES` listesindeki IP'lerden gelirse parse edilir (spoof koruması, rightmost-untrusted algoritması).
