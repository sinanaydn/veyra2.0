---
name: Veyra RentACar — Proje Mimarisi
description: Spring Boot 4.0.3 Maven Multi-Module RentACar REST API — güncel mimari kararlar, tamamlanan modüller, kalıplar ve sonraki adım
type: project
---

## Genel Bilgi

**Proje:** Veyra RentACar REST API  
**Dizin:** `c:\Veyra-RentACar\veyra-api\`  
**Kullanıcı:** Junior Java geliştiricisi, öğrenme amaçlı proje  
**Toplam Java dosyası:** ~85

---

## Teknoloji Yığını

| Teknoloji | Versiyon |
|---|---|
| Java | 25 |
| Spring Boot | 4.0.3 |
| PostgreSQL | 17 (Docker) |
| Maven | Multi-Module |
| Lombok | 1.18.38 |
| MapStruct | 1.6.3 |
| JJWT | 0.12.6 |
| SpringDoc OpenAPI | 2.8.6 |

---

## Modül Durumu

| Modül | Durum | Dosya Sayısı |
|-------|-------|-------------|
| veyra-core | ✅ Tamamlandı | 12 |
| veyra-auth | ✅ Tamamlandı | 16 |
| veyra-user | ✅ Tamamlandı | 9 |
| veyra-vehicle | ✅ Tamamlandı | 31 (brand+model+car) |
| veyra-rental | ✅ Tamamlandı | 10 |
| veyra-payment | ✅ Tamamlandı | 10 (entity, repo, rules, dto×2, mapper, service, manager, controller, enum) |
| veyra-app | ✅ Tamamlandı | 2 |

---

## Modül Bağımlılık Grafiği

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

## Her Alt-Domain İçin Paket Düzeni

```
com.veyra.<modül>.<domain>/
├── entity/      ← @SQLRestriction("deleted = false") ZORUNLU
├── repository/
├── service/     ← interface (DIP)
├── manager/     ← implements service
├── rules/       ← getByIdOrThrow + iş kuralları
├── controller/
├── dto/request/ & dto/response/
├── mapper/      ← MapStruct
└── enums/
```

---

## Temel Mimari Kararlar

### 1. Rules Deseni — getByIdOrThrow
```java
// Her Rules sınıfı fetch + kontrol birleştiriyor
public Brand getByIdOrThrow(Long id) {
    return brandRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(ErrorCodes.BRAND_NOT_FOUND, "..."));
}
// Manager repository.findById() YAZMAZ — Rules'a devreder
```

### 2. Update Uniqueness — AndIdNot
```java
// Repository
boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
// Rules — kendi kaydını false positive olarak saymaz
public void checkIfBrandNameExistsForUpdate(String name, Long id) { ... }
```

### 3. Soft Delete
- `deleted = true` + `save()` — asla `deleteById()`
- `@SQLRestriction("deleted = false")` tüm entity'lerde mevcut: `Brand`, `CarModel`, `Car`, `User`, `AuthUser`, `Rental`

### 4. Exception Hiyerarşisi
```
BusinessException (abstract)
├── ResourceNotFoundException  → 404
├── AlreadyExistsException     → 409
├── UnauthorizedException      → 401
├── ForbiddenException         → 403
└── BusinessRuleException      → 422  ← iş kuralı ihlali
```

### 5. @Transactional Kuralı
- Yazma: `@Transactional`
- Okuma: `@Transactional(readOnly = true)`

### 6. AdminSeeder
`veyra-auth/config/AdminSeeder.java` — ApplicationRunner, idempotent.
İlk başlatmada `admin@veyra.com` / `Admin1234!` oluşturur.

### 7. Modüler Mimari — Soft Reference
`Rental.carId` ve `Rental.userId` JPA FK değil — modüller arası bağımlılığı gevşetir.
Aynı şekilde `AuthUser.userId` → `User` için soft reference.

---

## Enum Lokasyonları

| Enum | Paket | Değerler |
|------|-------|---------|
| `Role` | `veyra-auth.role` | ADMIN, USER |
| `CarStatus` | `veyra-vehicle.car.enums` | AVAILABLE, RENTED, MAINTENANCE |
| `RentalStatus` | `veyra-rental.enums` | ACTIVE, COMPLETED, CANCELLED |
| `PaymentStatus` | `veyra-payment.enums` | PENDING, COMPLETED, FAILED, REFUNDED |

---

## Car Entity Alanları

`year` (int), `doors` (int), `baggages` (int), `dailyPrice` (BigDecimal), `status` (CarStatus)
**Plaka YOK, renk YOK.**
Zincir: `Car` → `CarModel` → `Brand`

---

## Kiralama İş Akışı

```
create → tarih kontrolü → araç müsait mi → aktif kiralama var mı → kullanıcı var mı
       → totalPrice = gün × dailyPrice → kaydet → car.status = RENTED

complete(id) → aktif mi → rental.COMPLETED + car.AVAILABLE
cancel(id)   → aktif mi → rental.CANCELLED + car.AVAILABLE
```

---

## Payment İş Akışı

```
pay(rentalId):
  1. rentalRules.getByIdOrThrow(rentalId) → kiralama var mı
  2. rentalRules.checkIfRentalIsActive(rental) → aktif mi
  3. paymentRules.checkIfAlreadyPaid(rentalId) → zaten ödendi mi
  4. amount = rental.totalPrice
  5. Payment kaydet (status: COMPLETED)
```

---

## Sıradaki Adım

**Backend tamamlandı.** Tüm 7 modül implement edildi (~95 Java dosyası).

Sonraki aşama:
1. **Pagination desteği** — tüm `getAll` endpoint'leri `PageResponse<T>` + `Pageable` ile
2. **Frontend implementasyonu** — `veyra-frontend/` (React + TypeScript + Vite)

---

## Roller ve Yetki Özeti

| Endpoint grubu | USER | ADMIN |
|---------------|------|-------|
| `GET /brands,/models,/cars` | ✅ | ✅ |
| `POST/PUT/DELETE /brands,/models,/cars` | ❌ | ✅ |
| `POST /rentals` (oluştur) | ✅ | ✅ |
| `POST /rentals/{id}/cancel` | ✅ | ✅ |
| `POST /rentals/{id}/complete` | ❌ | ✅ |
| `GET /rentals/my` | ✅ | ✅ |
| `GET /rentals` (tümü) | ❌ | ✅ |
| `GET /users/**` | ❌ | ✅ |

---

## Why / How to Apply

**Why:** Kullanıcı junior seviyede, her modülü eksiksiz tamamlayarak ilerliyor.
Mimari kararlar oturmuş — yeni session'da bu kararlara uygun devam et, onaylanmış kararları tekrar sorgulama.
**How to apply:** Yeni domain eklerken paket düzenine, Rules/Manager/Service ayrımına, `@SQLRestriction`, `getByIdOrThrow`, `BusinessRuleException` ve `@Transactional(readOnly)` kurallarına uy.