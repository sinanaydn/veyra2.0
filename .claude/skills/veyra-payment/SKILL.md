---
name: veyra-payment
description: Ödeme simülasyonu modülü. Payment entity, pay(rentalId) akışı. Aktif kiralamalar için tek ödeme yapılır. RentalRules cross-module çağırır. Gerçek ödeme sağlayıcısı yok — sadece kayıt tutar.
---

## Sorumluluk
Kiralamalara ait ödemeleri kaydeder. Gerçek bir ödeme gateway'i çağrısı **yok** — simülasyon. Aktif bir rental için ödeme `COMPLETED` status'üyle kaydedilir. Aynı rental'a ikinci ödeme engellenir.

## Payment Entity

| Alan | Tip | Notlar |
|------|-----|--------|
| `rentalId` | `Long` | **Soft reference** |
| `userId` | `Long` | **Soft reference** — `rental.userId`'den kopyalanır |
| `amount` | `BigDecimal(10,2)` | `rental.totalPrice`'tan kopyalanır |
| `status` | `PaymentStatus` | default `COMPLETED` (simülasyon) |

## PaymentStatus Enum

| Değer | Açıklama |
|-------|---------|
| `PENDING` | Ödeme bekleniyor (şu an kullanılmıyor) |
| `COMPLETED` | Ödeme tamamlandı |
| `FAILED` | Ödeme başarısız (şu an kullanılmıyor) |
| `REFUNDED` | İade edildi (şu an kullanılmıyor) |

Şu an sadece `COMPLETED` aktif. Gerçek ödeme gateway entegrasyonunda diğerleri kullanılacak.

## PaymentRules

| Metot | Davranış |
|-------|---------|
| `getByIdOrThrow(id)` | Yoksa `PAYMENT_NOT_FOUND` |
| `checkIfAlreadyPaid(rentalId)` | `existsByRentalIdAndStatus(rentalId, COMPLETED)` true ise `PAYMENT_ALREADY_DONE` (422) |

## PaymentRepository
| Metot | Açıklama |
|-------|---------|
| `findAllByUserId(userId)` | `getMyPayments` |
| `findAllByRentalId(rentalId)` | İleride kullanım için |
| `existsByRentalIdAndStatus(rentalId, status)` | Çift ödeme kontrolü |

## İş Akışı

### pay(request, email, isAdmin) — USER + ADMIN
```
1. rental = rentalRules.getByIdOrThrow(request.rentalId)
2. currentUserId = userRules.getUserIdByEmail(email)
3. isAdmin? → ownership kontrolü atlanır
   değilse → rental.userId != currentUserId ise 403
4. rentalRules.checkIfRentalIsActive(rental)      → ACTIVE değilse 422
5. paymentRules.checkIfAlreadyPaid(rental.id)     → zaten ödenmişse 422
6. Payment kaydet:
     rentalId = rental.id
     userId   = rental.userId                     ← manipülasyona karşı rental'dan alınır
     amount   = rental.totalPrice
     status   = COMPLETED
```

**ADMIN**, başka kullanıcının kiralaması için ödeme yapabilir (örn. manuel işlem).
`amount` istemci tarafından gönderilmez — `rental.totalPrice`'tan alınır.

### getById(id, email, isAdmin) — USER + ADMIN
```
1. payment = paymentRules.getByIdOrThrow(id)
2. isAdmin? → ownership kontrolü atlanır
   değilse → payment.userId != currentUserId ise 403
```

### getMyPayments(email)
```
1. userId = userRules.getUserIdByEmail(email)
2. findAllByUserId(userId)
```

## Endpoint'ler

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/payments` | USER+ADMIN | Ödeme oluştur — USER kendi kirasını öder, ADMIN hepsini |
| GET | `/api/v1/payments/{id}` | USER+ADMIN | Tek kayıt — USER yalnızca kendine ait görebilir |
| GET | `/api/v1/payments/my` | USER+ADMIN | Kendi ödemeler |
| GET | `/api/v1/payments?userId=X` | **ADMIN** | Tüm/filtreli liste |

### CreatePaymentRequest
- `rentalId` `@NotNull`

Sadece bu alan. Tutar ve userId backend'de hesaplanır.

## Güvenlik & Yetki Pattern'i
- Controller: `SecurityUtils.isAdmin(authentication)` ile rol kontrolü
- Service: `(request, email, isAdmin)` imzası
- Manager: `isAdmin` true ise ownership atlanır; false ise `getUserIdByEmail(email)` → `rental.userId` karşılaştırması

## Bağımlılıklar
- `veyra-core` — SecurityUtils, ForbiddenException, ErrorCodes
- `veyra-rental` — `RentalRules`, `Rental` entity
- `veyra-user` — `UserRules`
