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
| `exp` | Expiration — **24 saat** |

Algoritma: **HS256**. Secret `application.yml`'den (`jwt.secret`).

## SecurityConfig

- **Stateless** — `SessionCreationPolicy.STATELESS`
- **CSRF disabled** (JWT için doğru)
- **CORS** — `localhost:3000`, `localhost:5173` allowed
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

Diğer her şey authenticated olmalı.

## JwtAuthenticationFilter
Her request için:
1. `Authorization: Bearer <token>` header'ını oku
2. Yoksa filter chain'e devam et (public endpoint olabilir)
3. Token varsa parse et, claims'i çıkar
4. `SecurityContextHolder`'a `Authentication` koy (`email` principal, `role` authority)
5. Hata olursa 401 dönmesi `GlobalExceptionHandler`'a bırakılır

## AdminSeeder
`ApplicationRunner` implementasyonu, idempotent.

İlk başlatmada:
- `admin@veyra.com` / `Admin1234!` `AuthUser` oluşturur
- İlişkili `User` kaydı oluşturur
- Eğer zaten varsa hiçbir şey yapmaz (`existsByEmail` kontrolü)

## Endpoint'ler

| Method | Path | Auth | Body | Açıklama |
|--------|------|------|------|---------|
| POST | `/api/v1/auth/register` | Public | `RegisterRequest` | Yeni kullanıcı + AuthUser oluştur |
| POST | `/api/v1/auth/login` | Public | `LoginRequest` | Token döndür |

### RegisterRequest
- `firstName`, `lastName` — `@NotBlank`
- `email` — `@NotBlank` `@Email`
- `password` — `@NotBlank` `@Size(min=8)`
- `phone` — opsiyonel

Akışı:
1. `userRules.checkIfEmailAlreadyExists(email)` → 409 olabilir
2. `User` kaydı oluştur
3. BCrypt ile şifre hash'le
4. `AuthUser` kaydı oluştur (`userId` link)
5. JWT üret ve dön

### LoginRequest
- `email`, `password` — `@NotBlank`

Akışı:
1. `AuthUser`'ı email ile bul (yoksa `INVALID_CREDENTIALS`)
2. BCrypt ile şifre karşılaştır
3. JWT üret ve dön (`AuthResponse`: `token`, `email`, `role`)

## Bağımlılıklar
- `veyra-core` — `ApiResponse`, exceptions
- `veyra-user` — `UserRules`, `User` entity (register sırasında)
