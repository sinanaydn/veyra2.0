# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

All Maven commands must be run from `veyra-api/`:

```bash
# Build entire project
mvn clean install -DskipTests

# Run the application locally (requires Docker PostgreSQL running)
mvn -pl veyra-app spring-boot:run

# Run with Docker (PostgreSQL only — app runs locally)
docker-compose up -d
```

Default server: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

Default admin credentials (seeded on first start):
- Email: `admin@veyra.com`
- Password: `Admin1234!`

---

## Module Status

| Module | Status | Notes |
|--------|--------|-------|
| `veyra-core` | ✅ Complete | BaseEntity, ApiResponse, exceptions, GlobalExceptionHandler, ErrorCodes, ApiConstants |
| `veyra-auth` | ✅ Complete | JWT, SecurityConfig, AdminSeeder, register/login |
| `veyra-user` | ✅ Complete | CRUD, UserRules, UserMapper |
| `veyra-vehicle` | ✅ Complete | brand + model + car — tüm katmanlar |
| `veyra-rental` | ✅ Complete | Kiralama oluştur, iade, iptal, kendi kiralamaları |
| `veyra-payment` | ⏳ Next | Placeholder — henüz implement edilmedi |
| `veyra-app` | ✅ Complete | VeyraApplication, SwaggerConfig, application.yml |

---

## Module Architecture

Multi-module Maven project under `veyra-api/`:

| Module | Purpose |
|--------|---------|
| `veyra-core` | `BaseEntity`, `ApiResponse<T>`, `PageResponse<T>`, exceptions (`BusinessRuleException` dahil), `ErrorCodes`, `GlobalExceptionHandler` |
| `veyra-auth` | JWT auth, `JwtAuthenticationFilter`, `SecurityConfig`, `AdminSeeder` |
| `veyra-user` | User profile CRUD — auth credentials bu modülde değil |
| `veyra-vehicle` | `brand/` + `model/` + `car/` — tam implement |
| `veyra-rental` | `Rental` entity, kiralama akışı — `CarRules` + `UserRules` kullanır |
| `veyra-payment` | Ödeme simülasyonu — **henüz boş** |
| `veyra-app` | Spring Boot entry point; aggregates all modules |

**Dependency direction (unidirectional):**
```
veyra-app → tüm modüller
veyra-payment → veyra-core, veyra-rental, veyra-vehicle
veyra-rental  → veyra-core, veyra-vehicle, veyra-user
veyra-vehicle → veyra-core
veyra-user    → veyra-core
veyra-auth    → veyra-core, veyra-user
veyra-core    → (hiçbir şey)
```

---

## Package Layout (per domain module)

```
com.veyra.<module>.<domain>/
├── entity/       # JPA entities (@SQLRestriction("deleted = false") zorunlu)
├── repository/   # Spring Data JPA repositories
├── service/      # Interfaces — controllers depend on these, never the impl
├── manager/      # Service implementations
├── rules/        # Business rule validation — getByIdOrThrow pattern kullanılır
├── controller/   # REST endpoints
├── dto/
│   ├── request/
│   └── response/
├── mapper/       # MapStruct
└── enums/
```

---

## Key Patterns

### Rules Pattern — getByIdOrThrow
Her Rules sınıfında hem varlık kontrolü hem fetch tek metotta:
```java
public Brand getByIdOrThrow(Long id) {
    return brandRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BRAND_NOT_FOUND, "..."));
}
```
Manager asla `repository.findById().orElseThrow()` yazmaz — Rules'a devreder.

### Update Uniqueness Check
Update'te `AndIdNot` kullanılır — aksi hâlde aynı isimde kendi kaydı false positive verir:
```java
// Repository
boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
// Rules
public void checkIfBrandNameExistsForUpdate(String name, Long id) { ... }
```

### Soft Delete
- `BaseEntity.deleted = false` → `setDeleted(true)` + `save()` — asla `deleteById()`
- Her entity'de `@SQLRestriction("deleted = false")` zorunlu — aksi hâlde `findAll()` silinmiş kayıtları getirir

### Exception Hierarchy
```
BusinessException (abstract)
├── ResourceNotFoundException  → 404
├── AlreadyExistsException     → 409
├── UnauthorizedException      → 401
├── ForbiddenException         → 403
└── BusinessRuleException      → 422  ← iş kuralı ihlali (araç müsait değil, tarih geçersiz vb.)
```
`GlobalExceptionHandler` tümünü yakalar. Controller ve Manager'da try-catch yok.

### ApiResponse Envelope
```json
{ "success": true, "status": 200, "message": "...", "data": {...}, "errorCode": null, "timestamp": "..." }
```

### @Transactional Kuralı
- Yazma metotları: `@Transactional`
- Okuma metotları: `@Transactional(readOnly = true)`

---

## API Base Paths

| Path | Module | Auth |
|------|---------|------|
| `/api/v1/auth/**` | veyra-auth | Public |
| `/api/v1/users/**` | veyra-user | ADMIN only |
| `/api/v1/brands/**` | veyra-vehicle | GET: authenticated, POST/PUT/DELETE: ADMIN |
| `/api/v1/models/**` | veyra-vehicle | GET: authenticated, POST/PUT/DELETE: ADMIN |
| `/api/v1/cars/**` | veyra-vehicle | GET: authenticated, POST/PUT/DELETE: ADMIN |
| `/api/v1/rentals/**` | veyra-rental | GET/POST/cancel: USER+ADMIN, complete: ADMIN |
| `/api/v1/payments/**` | veyra-payment | Henüz implement edilmedi |

---

## Database & Auth

- **PostgreSQL 17** — Docker ile çalıştırılır, uygulama lokalde (`mvn spring-boot:run`)
- `ddl-auto: update` — dev ortamı; production'da `validate`
- **Soft delete** — `deleted = true`, asla `DELETE FROM`
- **JWT** — stateless; token: `email`, `userId`, `role`; 24 saat geçerli
- **AdminSeeder** — ilk başlatmada `admin@veyra.com` / `Admin1234!` otomatik oluşturulur
- `AuthUser` ve `User` ayrı tablolar — `AuthUser.userId` soft reference (JPA FK değil)

---

## Enum Locations

| Enum | Lokasyon | Değerler |
|------|----------|---------|
| `Role` | `veyra-auth/.../role/Role.java` | `ADMIN`, `USER` |
| `CarStatus` | `veyra-vehicle/.../car/enums/CarStatus.java` | `AVAILABLE`, `RENTED`, `MAINTENANCE` |
| `RentalStatus` | `veyra-rental/.../enums/RentalStatus.java` | `ACTIVE`, `COMPLETED`, `CANCELLED` |
| `PaymentStatus` | `veyra-payment/.../enums/PaymentStatus.java` | Henüz oluşturulmadı |

---

## Rental Business Flow

```
create():
  1. checkIfDatesValid(start, end)
  2. carRules.getByIdOrThrow(carId) → araç var mı
  3. carRules.checkIfCarAvailable(car) → AVAILABLE mı
  4. rentalRules.checkIfCarAlreadyRented(carId) → aktif kiralama var mı
  5. userRules.checkIfUserExists(userId)
  6. totalPrice = günSayısı × dailyPrice
  7. Rental kaydet + car.status = RENTED

complete(id):
  1. rentalRules.getByIdOrThrow(id)
  2. rentalRules.checkIfRentalIsActive(rental)
  3. rental.status = COMPLETED + car.status = AVAILABLE

cancel(id):
  1. rentalRules.getByIdOrThrow(id)
  2. rentalRules.checkIfRentalIsActive(rental)
  3. rental.status = CANCELLED + car.status = AVAILABLE
```

---

## Sıradaki Adım

**`veyra-payment`** implement edilecek:
- `PaymentStatus` enum: `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED`
- `Payment` entity: `rentalId` (soft ref), `userId` (soft ref), `amount`, `status`
- `PaymentRules`: `checkIfPaymentAlreadyDone`, `getByIdOrThrow`
- `PaymentManager.pay(rentalId)`: kiralama var mı + zaten ödendi mi → ödeme oluştur
- `GET /api/v1/payments/{id}`, `POST /api/v1/payments` endpoints
