---
name: veyra-rental
description: Kiralama akışı modülü. Rental entity, create/complete/cancel iş akışları. CarRules ve UserRules cross-module çağırır. Car durumunu RENTED↔AVAILABLE arası değiştirir. RentalRules cross-module olarak veyra-payment tarafından kullanılır.
---

## Sorumluluk
Kiralama operasyonlarını yönetir. Bir araç kiralandığında `Car.status = RENTED` yapar; iade/iptal sonrası `AVAILABLE`'a döner. Toplam fiyat hesaplaması burada yapılır.

## Rental Entity

| Alan | Tip | Notlar |
|------|-----|--------|
| `carId` | `Long` | **Soft reference** — JPA FK yok |
| `userId` | `Long` | **Soft reference** — JPA FK yok |
| `startDate` | `LocalDate` | |
| `endDate` | `LocalDate` | |
| `totalPrice` | `BigDecimal(10,2)` | Hesaplanır |
| `status` | `RentalStatus` | default `ACTIVE` |

Soft reference: modüller arası bağımlılığı gevşetmek için JPA `@ManyToOne` yerine sadece ID tutulur.

## RentalStatus Enum

| Değer | Açıklama |
|-------|---------|
| `ACTIVE` | Devam eden kiralama |
| `COMPLETED` | Araç iade edildi |
| `CANCELLED` | İptal edildi |

## RentalRules (cross-module)
`veyra-payment.PaymentManager` tarafından kullanılır.

| Metot | Davranış |
|-------|---------|
| `getByIdOrThrow(id)` | Yoksa `RENTAL_NOT_FOUND` |
| `checkIfCarAlreadyRented(carId)` | Aktif kiralama varsa `RENTAL_ALREADY_ACTIVE` (422) |
| `checkIfRentalIsActive(rental)` | `ACTIVE` değilse `RENTAL_NOT_ACTIVE` (422) |
| `checkIfDatesValid(start, end)` | `end <= start` ise `RENTAL_DATE_INVALID` (422) |

## RentalRepository
| Metot | Açıklama |
|-------|---------|
| `findAllByUserId(userId)` | `getMyRentals` ve admin filter |
| `findAllByCarId(carId)` | Araç bazlı geçmiş |
| `existsByCarIdAndStatus(carId, status)` | Aktif kiralama kontrolü |

## İş Akışları

### create(request, email) — USER + ADMIN
```
1. checkIfDatesValid(start, end)
2. car = carRules.getByIdOrThrowForUpdate(carId)   ← Pessimistic Lock (SELECT FOR UPDATE)
3. carRules.checkIfCarAvailable(car)
4. rentalRules.checkIfCarAlreadyRented(carId)
5. userId = userRules.getUserIdByEmail(email)       ← Authentication'dan türetilir, client'tan ALINMAZ
6. days = ChronoUnit.DAYS.between(start, end)
7. totalPrice = car.dailyPrice × days
8. Rental kaydet (status=ACTIVE)
9. carService.markAsRented(carId)              ← CarService interface üzerinden (DIP)
```

**Race condition koruması:** `getByIdOrThrowForUpdate` araç satırını kilitler.
Eş zamanlı ikinci istek bu lock'u bekler; ilk commit'ten sonra `checkIfCarAvailable` veya
`checkIfCarAlreadyRented` ile güvenli 422 üretir.

### complete(id) — sadece ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. rentalRules.checkIfRentalIsActive(rental)
3. rental.status = COMPLETED
4. carService.markAsAvailable(rental.carId)    ← CarService interface üzerinden (DIP)
```

### cancel(id, email, isAdmin) — USER + ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. SecurityUtils.checkOwnership(rental.userId, email, isAdmin, userRules::getUserIdByEmail)
3. rentalRules.checkIfRentalIsActive(rental)
4. rental.status = CANCELLED
5. carService.markAsAvailable(rental.carId)
```

### getById(id, email, isAdmin) — USER + ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. SecurityUtils.checkOwnership(rental.userId, email, isAdmin, userRules::getUserIdByEmail)
3. response döndür
```

### getMyRentals(email) / getMyRentals(email, pageable)
```
1. userId = userRules.getUserIdByEmail(email)
2. findAllByUserId(userId) → response listesi (paginated veya tam)
```

`Authentication.getName()` JWT'den email'i verir.

## Endpoint'ler

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/rentals` | USER+ADMIN | Yeni kiralama — userId authentication'dan alınır |
| POST | `/api/v1/rentals/{id}/complete` | **ADMIN** | İade |
| POST | `/api/v1/rentals/{id}/cancel` | USER+ADMIN | İptal — USER yalnızca kendi kiraladığını iptal edebilir |
| GET | `/api/v1/rentals/{id}` | USER+ADMIN | Tek kayıt — USER yalnızca kendine ait görebilir |
| GET | `/api/v1/rentals/my` | USER+ADMIN | Kendi kiralamaları (pageable: `?page=0&size=20`) |
| GET | `/api/v1/rentals?userId=X` | **ADMIN** | Tüm/filtreli liste (pageable: `?page=0&size=20`) |

### CreateRentalRequest
- `carId` `@NotNull`
- `startDate` `@NotNull` `@FutureOrPresent`
- `endDate` `@NotNull` `@Future`

**`userId` alanı yoktur** — kullanıcı kimliği her zaman JWT'den türetilir.

## Güvenlik & Yetki Pattern'i
- Controller: `Authentication authentication` inject → `SecurityUtils.isAdmin(authentication)` ile rol kontrolü
- Service: `(id, email, isAdmin)` imzası
- Manager: `SecurityUtils.checkOwnership(entityUserId, email, isAdmin, userRules::getUserIdByEmail)` — merkezi ownership kontrolü

## DB Index'leri
- `idx_rental_car_status(carId, status, deleted)` — aktif kiralama kontrolü
- `idx_rental_user_status(userId, status, deleted)` — kullanıcı bazlı listeleme

## Bağımlılıklar
- `veyra-core` — SecurityUtils, ForbiddenException, ErrorCodes
- `veyra-vehicle` — `CarRules` (lock + validasyon), `CarService` (status değişikliği — DIP)
- `veyra-user` — `UserRules`
