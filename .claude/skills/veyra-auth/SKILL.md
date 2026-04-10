---
name: veyra-auth
description: JWT tabanlı stateless authentication. SecurityConfig, JwtAuthenticationFilter, AuthUser entity, register/login endpoint'leri ve AdminSeeder içerir. AuthUser ile veyra-user'daki User ayrı tablolardır.
---

## Sorumluluk
Kimlik doğrulama ve yetkilendirme. Şifre hash'leme (BCrypt), JWT üretme/doğrulama, role-based access control. Profil bilgileri burada **tutulmaz** — sadece auth credentials. Profil için `veyra-user`.

## AuthUser ↔ User Ayrımı
İki ayrı tablo:
- **`AuthUser`** (bu modül): `email`, `passwordHash`, `role`, `userId` (soft reference)
- **`User`** (veyra-user): `firstName`, `lastName`, `email`, `phone`

`AuthUser.userId` JPA FK **değildir** — modüller arası bağımlılığı gevşetmek için soft reference. Register sırasında önce `User` oluşturulur, sonra `AuthUser` ona link'lenir.

## Role Enum
`com.veyra.auth.role.Role`:
- `ADMIN`
- `USER`

JWT içinde `role` claim olarak taşınır. `@PreAuthorize("hasRole('ADMIN')")` ile kullanılır.

## JWT Yapısı

| Claim | Açıklama |
|-------|---------|
| `sub` | Kullanıcı email'i |
| `userId` | `User.id` (soft reference) |
| `role` | `ADMIN` veya `USER` |
| `iat` | Issued at |
| `exp` | Expiration — **15 dakika** |

Algoritma: **HS256**. Secret `application.yml`'den (`jwt.secret`).

## Refresh Token

DB tabanlı, revoke edilebilir. `refresh_tokens` tablosuna yazılır.

| Alan | Notlar |
|------|--------|
| `token` | UUID, unique |
| `authUserId` | AuthUser'a soft reference |
| `expiresAt` | 7 gün |

**Token rotation:** Her `/auth/refresh` isteğinde önce yeni token oluşturulur, sonra eski silinir (create-first-delete-after — atomic güvenlik).
**Scheduled cleanup:** `RefreshTokenCleanupTask` her 6 saatte expired token'ları temizler.
**Global logout:** `/auth/logout` o kullanıcının tüm refresh token'larını siler.
**Login:** Yeni login önceki tüm token'ları temizler (global logout tutarlılığı).

## SecurityConfig

- **Stateless** — `SessionCreationPolicy.STATELESS`
- **CSRF disabled** (JWT için doğru)
- **CORS** — `CORS_ALLOWED_ORIGINS` env var'dan okunur (default: `localhost:3000,5173`)
- `@EnableMethodSecurity` — `@PreAuthorize` aktif
- `JwtAuthenticationFilter` `UsernamePasswordAuthenticationFilter`'dan önce

### Public Endpoint'ler (token gerekmez)
```
/api/v1/auth/**
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
/webjars/**
/actuator/health
```

Özel yetki kuralları:
- `DELETE /**` → `hasRole('ADMIN')`
- `/api/v1/admin/**` → `hasRole('ADMIN')`
- `/api/v1/users/**` → `hasRole('ADMIN')`
- Diğer her şey authenticated olmalı.

## JwtAuthenticationFilter
Her request için:
1. `Authorization: Bearer <token>` header'ını oku
2. Yoksa filter chain'e devam et (public endpoint olabilir)
3. Token varsa parse et, claims'i çıkar
4. `SecurityContextHolder`'a `Authentication` koy (`email` principal, `role` authority)
5. `JwtException | IllegalArgumentException` yakalanırsa filter zinciri devam eder ve return edilir — SecurityContext boş kalır, Spring Security korumalı endpoint'e 401 döndürür. `GlobalExceptionHandler` devrede değildir.

## AdminSeeder
`ApplicationRunner` implementasyonu, idempotent.

İlk başlatmada:
- `ADMIN_EMAIL` (varsayılan `admin@veyra.com`) ve `ADMIN_PASSWORD` env var'larından credentials alır
- `ADMIN_PASSWORD` set edilmezse `IllegalStateException` fırlatarak startup'ı durdurur
- `AuthUser` (ADMIN rolü) ve ilişkili `User` kaydı oluşturur
- Eğer zaten varsa hiçbir şey yapmaz (`existsByEmail` kontrolü)

## Endpoint'ler

| Method | Path | Auth | Body | Açıklama |
|--------|------|------|------|---------|
| POST | `/api/v1/auth/register` | Public | `RegisterRequest` | Yeni kullanıcı + AuthUser oluştur |
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | Access + refresh token döndür |
| POST | `/api/v1/auth/refresh` | Public | `RefreshRequest` | Token rotation — yeni access + refresh token |
| POST | `/api/v1/auth/logout` | Public | `RefreshRequest` | Kullanıcının tüm refresh token'larını sil |

### RegisterRequest
- `firstName`, `lastName` — `@NotBlank` `@Size(max=50)`
- `email` — `@NotBlank` `@Email` `@Size(max=255)`
- `password` — `@NotBlank` `@Size(min=10, max=128)` `@Pattern(büyük+küçük+rakam+özel karakter)`
- `phone` — opsiyonel `@Size(max=15)` `@Pattern(telefon formatı)`

Akışı:
1. `userRules.checkIfEmailAlreadyExists(email)` → 409 olabilir
2. `User` kaydı oluştur
3. BCrypt ile şifre hash'le
4. `AuthUser` kaydı oluştur (`userId` link)
5. JWT üret ve dön

### LoginRequest
- `email` — `@NotBlank` `@Email` `@Size(max=255)`
- `password` — `@NotBlank` `@Size(max=128)`

Akışı:
1. `AuthUser`'ı email ile bul (yoksa `INVALID_CREDENTIALS`)
2. BCrypt ile şifre karşılaştır
3. Önceki tüm refresh token'ları temizle (`revokeAllByAuthUserId`)
4. Access token (15 dk) + refresh token (7 gün) üret
5. `AuthResponse`: `token`, `refreshToken`, `expiresIn`, `refreshExpiresIn`, `email`, `role`

### RefreshRequest
- `refreshToken` — `@NotBlank`

`/refresh` akışı:
1. `authRules.getRefreshTokenOrThrow(token)` — bulunamazsa 401
2. Süresi dolmuşsa DB'den sil → 401
3. Token rotation: eski sil, yeni oluştur
4. `AuthUser`'ı bul → yeni access token üret

`/logout` akışı:
1. Token'dan `authUserId` bul
2. Kullanıcının tüm refresh token'larını sil (idempotent — token yoksa sessizce geçer)

## AuthRules
| Metot | Davranış |
|-------|---------|
| `getByEmailOrThrow(email)` | AuthUser döndürür, yoksa `UnauthorizedException` (`INVALID_CREDENTIALS`) |
| `getByIdOrThrow(id)` | AuthUser döndürür, yoksa `UnauthorizedException` (`TOKEN_INVALID`) |
| `getByUserIdOrThrow(userId)` | AuthUser döndürür, yoksa `ResourceNotFoundException` (`AUTH_USER_NOT_FOUND`) |
| `getRefreshTokenOrThrow(token)` | RefreshToken döndürür, yoksa `UnauthorizedException` |
| `checkIfEmailAlreadyRegistered(email)` | Varsa `AlreadyExistsException` |
| `checkIfRoleAlreadyAssigned(authUser, role)` | Zaten aynı roldeyse `BusinessRuleException` (`ROLE_ALREADY_ASSIGNED`) |

`AuthManager` artık doğrudan repository çağrısı yapmaz — tüm entity fetch'ler `AuthRules` üzerinden yapılır.

## UserDeletedEventListener
`com.veyra.auth.event.UserDeletedEventListener` — `UserDeletedEvent` dinler.
- AuthUser'ı email ile bulur → `deleted = true`
- `refreshTokenService.revokeAllByAuthUserId()` ile tüm token'ları iptal eder
- Kullanıcı silme işlemi transactional olarak `veyra-user` → event → `veyra-auth` zincirinde çalışır

## AdminController — Role Management
`/api/v1/admin/**` path'i altında, sadece ADMIN erişimli.

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| PUT | `/api/v1/admin/users/{userId}/role` | **ADMIN** | Kullanıcı rolü değiştir |

### ChangeRoleRequest
- `role` — `@NotNull` (`ADMIN` veya `USER`)

Akışı: `AuthRules.getByUserIdOrThrow(userId)` → `checkIfRoleAlreadyAssigned` → `setRole` → save.

## RateLimitFilter
- `/login` ve `/register` endpoint'lerine uygulanır — IP başına 5 istek/dakika
- **X-Forwarded-For güvenliği:** Sadece trusted proxy'lerden (localhost) gelen XFF header'ına güvenilir, client doğrudan bağlanırsa `remoteAddr` kullanılır
- Her 5 dakikada stale entry'ler temizlenir (`@Scheduled`)

## Bağımlılıklar
- `veyra-core` — `ApiResponse`, exceptions, `UserDeletedEvent`
- `veyra-user` — `UserRules`, `User` entity (register sırasında)
