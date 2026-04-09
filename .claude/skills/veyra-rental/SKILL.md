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
9. car.status = RENTED → save
```

**Race condition koruması:** `getByIdOrThrowForUpdate` araç satırını kilitler.
Eş zamanlı ikinci istek bu lock'u bekler; ilk commit'ten sonra `checkIfCarAvailable` veya
`checkIfCarAlreadyRented` ile güvenli 422 üretir.

### complete(id) — sadece ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. rentalRules.checkIfRentalIsActive(rental)
3. rental.status = COMPLETED
4. car = carRules.getByIdOrThrow(rental.carId)
5. car.status = AVAILABLE → save
```

### cancel(id, email, isAdmin) — USER + ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. isAdmin? → ownership kontrolü atlanır
   değilse → userRules.getUserIdByEmail(email) → rental.userId ile karşılaştır → eşleşmezse 403
3. rentalRules.checkIfRentalIsActive(rental)
4. rental.status = CANCELLED
5. car.status = AVAILABLE → save
```

### getById(id, email, isAdmin) — USER + ADMIN
```
1. rental = rentalRules.getByIdOrThrow(id)
2. isAdmin? → ownership kontrolü atlanır
   değilse → userId = getUserIdByEmail(email) → rental.userId ile karşılaştır → eşleşmezse 403
3. response döndür
```

### getMyRentals(email)
```
1. userId = userRules.getUserIdByEmail(email)
2. findAllByUserId(userId) → response listesi
```

`Authentication.getName()` JWT'den email'i verir.

## Endpoint'ler

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/rentals` | USER+ADMIN | Yeni kiralama — userId authentication'dan alınır |
| POST | `/api/v1/rentals/{id}/complete` | **ADMIN** | İade |
| POST | `/api/v1/rentals/{id}/cancel` | USER+ADMIN | İptal — USER yalnızca kendi kiraladığını iptal edebilir |
| GET | `/api/v1/rentals/{id}` | USER+ADMIN | Tek kayıt — USER yalnızca kendine ait görebilir |
| GET | `/api/v1/rentals/my` | USER+ADMIN | Kendi kiralamaları |
| GET | `/api/v1/rentals?userId=X` | **ADMIN** | Tüm/filtreli liste |

### CreateRentalRequest
- `carId` `@NotNull`
- `startDate` `@NotNull` `@FutureOrPresent`
- `endDate` `@NotNull` `@Future`

**`userId` alanı yoktur** — kullanıcı kimliği her zaman JWT'den türetilir.

## Güvenlik & Yetki Pattern'i
- Controller: `Authentication authentication` inject → `SecurityUtils.isAdmin(authentication)` ile rol kontrolü
- Service: `(id, email, isAdmin)` imzası
- Manager: `isAdmin` true ise ownership kontrolü atlanır; false ise `getUserIdByEmail(email)` → entity.userId karşılaştırması

## Bağımlılıklar
- `veyra-core` — SecurityUtils, ForbiddenException, ErrorCodes
- `veyra-vehicle` — `CarRules`, `CarRepository` (status update + lock), `CarStatus`
- `veyra-user` — `UserRules`
