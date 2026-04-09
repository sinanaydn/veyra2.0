# CLAUDE.md

Bu dosya proje **geneline** ait bilgileri içerir. Modüle özgü detaylar için ilgili modülün içindeki `SKILL.md` dosyasına bakın.

---

## Build & Run

Tüm Maven komutları `veyra-api/` dizininden çalıştırılır.

```bash
mvn clean install -DskipTests           # Tüm projeyi build et
mvn -pl veyra-app spring-boot:run       # Uygulamayı lokalde çalıştır
docker-compose up -d                    # PostgreSQL'i Docker'da başlat
```

- Server: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui/index.html`

---

## Modüller

| Modül | Sorumluluk | Detay |
|-------|------------|-------|
| `veyra-core` | BaseEntity, ApiResponse, exceptions, ErrorCodes | `veyra-core/SKILL.md` |
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
veyra-vehicle → veyra-core
veyra-user    → veyra-core
veyra-auth    → veyra-core, veyra-user
veyra-core    → (hiçbir şey)
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

### `ApiResponse<T>` envelope
Tüm endpoint'ler `ApiResponse<T>` döndürür:
```json
{ "success": true, "status": 200, "message": "...", "data": {...}, "errorCode": null, "timestamp": "..." }
```

---

## API Base Paths

| Path | Modül |
|------|-------|
| `/api/v1/auth/**` | veyra-auth |
| `/api/v1/users/**` | veyra-user |
| `/api/v1/brands/**`, `/models/**`, `/cars/**` | veyra-vehicle |
| `/api/v1/rentals/**` | veyra-rental |
| `/api/v1/payments/**` | veyra-payment |

Endpoint detayları ve yetki kuralları için ilgili modülün `SKILL.md` dosyasına bakın.

---

## Veritabanı & Auth

- **PostgreSQL 17** — Docker, app lokalde
- `ddl-auto: update` (dev) / `validate` (prod)
- **JWT** — stateless; claims: `email`, `userId`, `role`; 24 saat
- **AdminSeeder** — ilk başlatmada `ADMIN_EMAIL` / `ADMIN_PASSWORD` env var'larından admin oluşturur; `ADMIN_PASSWORD` zorunludur, set edilmezse startup durur
