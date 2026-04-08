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
| `checkIfEmailAlreadyExists(email)` | Varsa `AlreadyExistsException` (`EMAIL_ALREADY_EXISTS`) |
| `checkIfUserExists(id)` | Yoksa `ResourceNotFoundException` (`USER_NOT_FOUND`) |
| `getUserIdByEmail(email)` | Email'den userId döndürür, yoksa `ResourceNotFoundException` |

`getByIdOrThrow` pattern'ı `User` entity'sini doğrudan dönmez — sadece var/yok kontrolü ve ID/email mapping yapar (cross-module'da entity sızdırmamak için).

## UserRepository
| Metot | Açıklama |
|-------|---------|
| `existsByEmail(String)` | Register sırasında uniqueness için |
| `findByEmail(String)` | `getUserIdByEmail` kullanır |

## Endpoint'ler

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/users` | ADMIN | Yeni kullanıcı (admin tarafından) |
| PUT | `/api/v1/users/{id}` | ADMIN | Güncelle |
| GET | `/api/v1/users/{id}` | ADMIN | Tek kayıt |
| GET | `/api/v1/users` | ADMIN | Tüm kayıtlar |
| DELETE | `/api/v1/users/{id}` | ADMIN | Soft delete |

**Tüm endpoint'ler ADMIN-only.** Kullanıcılar kendi profilini bu endpoint'lerden yönetmez — register `veyra-auth`'tan yapılır. Self-service profile update şu an yok.

## Bağımlılıklar
- `veyra-core`
