---
name: veyra-user
description: Kullanıcı profili CRUD modülü. Auth credentials burada DEĞİL — onlar veyra-auth'taki AuthUser'da. UserRules cross-module olarak veyra-rental ve veyra-payment tarafından kullanılır.
---

## Sorumluluk
Kullanıcı profilini yönetir. Sadece profil bilgileri (ad, soyad, telefon, email). Şifre/role burada **yok** — onlar `veyra-auth.AuthUser`'da.

## User Entity

| Alan | Tip | Notlar |
|------|-----|--------|
| `firstName` | `String` | `@NotBlank` |
| `lastName` | `String` | `@NotBlank` |
| `email` | `String` | Unique, `@Email` |
| `phone` | `String` | Opsiyonel |

`@SQLRestriction("deleted = false")` aktif. `BaseEntity` extend eder.

## UserRules
Cross-module kullanılan yardımcı sınıf — `veyra-rental` ve `veyra-payment` çağırır.

| Metot | Davranış |
|-------|---------|
| `getByIdOrThrow(id)` | `User` döndürür, yoksa `ResourceNotFoundException` (`USER_NOT_FOUND`) |
| `getByEmailOrThrow(email)` | `User` döndürür, yoksa `ResourceNotFoundException` (`USER_NOT_FOUND`) |
| `checkIfEmailAlreadyExists(email)` | Varsa `AlreadyExistsException` (`EMAIL_ALREADY_EXISTS`) |
| `checkIfUserExists(id)` | Yoksa `ResourceNotFoundException` (`USER_NOT_FOUND`) |
| `getUserIdByEmail(email)` | Email'den userId döndürür — `getByEmailOrThrow`'a delege eder |

`getByIdOrThrow` ve `getByEmailOrThrow` `User` entity döndürür (modül-içi kullanım). Cross-module'da `getUserIdByEmail` ve `checkIfUserExists` tercih edilir (entity sızdırmamak için).

## UserRepository
| Metot | Açıklama |
|-------|---------|
| `existsByEmail(String)` | Register sırasında uniqueness için |
| `findByEmail(String)` | `getUserIdByEmail` kullanır |

## User Silme Cascade (Event-Driven)
`UserManager`'da hem `delete(id)` (admin) hem `deleteByEmail(email)` (self) ortak bir `doDelete(User)` private helper'ına delege eder:
1. User soft-delete
2. `UserDeletedEvent(userId, email)` publish

`veyra-auth` modülündeki `UserDeletedEventListener` bu event'i dinler ve:
1. AuthUser'ı email ile bulur → soft delete
2. Tüm refresh token'ları revoke eder

Bu sayede silinen kullanıcı sisteme tekrar giriş yapamaz.

## Endpoint'ler

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| GET | `/api/v1/users` | ADMIN | Tüm kayıtlar (pageable: `?page=0&size=20&sort=createdAt,desc`) |
| GET | `/api/v1/users/{id}` | ADMIN | Tek kayıt |
| PUT | `/api/v1/users/{id}` | ADMIN | Profil güncelle (firstName, lastName, phone) |
| DELETE | `/api/v1/users/{id}` | ADMIN | Soft delete + AuthUser cascade |
| **DELETE** | **`/api/v1/users/me`** | **Authenticated** | **Kullanıcının kendi hesabını silmesi. JWT'den email alınır (id spoofing yok), aynı cascade akışı (AuthUser + token revoke) çalışır.** |

`SecurityConfig`'te `DELETE /users/me` matcher'ı `DELETE /**` = ADMIN kuralından **önce** yazılmıştır — ilk eşleşen kazanır.

### UpdateUserRequest
- `firstName` — `@NotBlank` `@Size(max=50)`
- `lastName` — `@NotBlank` `@Size(max=50)`
- `phone` — opsiyonel `@Size(max=15)` `@Pattern(telefon formatı)`

**Email güncellemesi yapılmaz** — AuthUser ile sync sorunu olur.

**`/me` dışındaki endpoint'ler ADMIN-only.** Register `veyra-auth`'tan yapılır.

### Bilinen Eksikler (PROGRESS.md)
- Son admin koruması — admin kendi hesabını `/me` üzerinden silebiliyor
- Aktif kiralama kontrolü — aktif rental'ı olan kullanıcı silinirse orphan rental riski var

## Bağımlılıklar
- `veyra-core`
