# Veyra API — Güvenlik İyileştirme Raporu

## Tamamlanan Görevler

### Kritik Açıklar ✅

| # | Açık | Değişen Dosyalar |
|---|------|-----------------|
| 1 | **IDOR — Payment getById** | `PaymentController`, `PaymentService`, `PaymentManager` |
| 2 | **Payment ownership (pay)** | `PaymentController`, `PaymentService`, `PaymentManager` |
| 3 | **Rental userId client'tan alınıyordu** | `CreateRentalRequest` (userId silindi), `RentalController`, `RentalService`, `RentalManager` |
| 4 | **Rental cancel ownership yok** | `RentalController`, `RentalService`, `RentalManager` |
| 5 | **Race condition — eş zamanlı kiralama** | `CarRepository` (`findByIdForUpdate` + `@Lock`), `CarRules` (`getByIdOrThrowForUpdate`), `RentalManager.create` |

### Yüksek Öncelik ✅

| # | Açık | Değişen Dosyalar |
|---|------|-----------------|
| 6 | **JWT Filter exception handling** | `JwtAuthenticationFilter` (try/catch JwtException) |
| 7 | **Admin credentials hardcoded** | `application.yml` (fallback kaldırıldı), `AdminSeeder` (startup validasyon) |
| 8 | **JWT secret hardcoded fallback** | `application.yml` (fallback kaldırıldı), `JwtServiceImpl` (@PostConstruct byte-length validasyon) |
| 9 | **Refresh token / logout yok** | `RefreshToken` entity, `RefreshTokenRepository`, `RefreshTokenService/Manager`, `AuthManager`, `AuthController` (+/refresh +/logout), `application.yml` (15 dk access / 7 gün refresh) |

### Refactor / Pattern ✅

| # | İyileştirme | Değişen Dosyalar |
|---|-------------|-----------------|
| R1 | **SecurityUtils.isAdmin** — kod tekrarı giderildi | `veyra-core/util/SecurityUtils.java` (yeni), her iki Controller güncellendi |
| R2 | **PaymentManager.pay isAdmin bypass** | `PaymentService`, `PaymentManager`, `PaymentController` |
| R3 | **getUserIdByEmail admin path'te çağrılmıyordu** | `PaymentManager.pay` |
| R4 | **AuthRules.getRefreshTokenOrThrow** — CLAUDE.md ihlali giderildi | `AuthRules`, `RefreshTokenManager` |
| R5 | **Login'de önceki token'lar temizleniyor** | `AuthManager.login` |

### Testler ✅
- `RentalManagerOwnershipTest` — 6 test (create/cancel/getById ownership + admin bypass)
- `PaymentManagerOwnershipTest` — 6 test (pay/getById ownership + admin bypass)

---

## Kalan Görevler (Orta Öncelik)

### 5. Payment Idempotency Key
**Durum:** Yapılmadı  
**Sorun:** Network hatası sonrası client retry'da `PAYMENT_ALREADY_DONE` 422 alıyor. `X-Idempotency-Key` header ile aynı isteğin tekrarında cached response döndürülmeli.  
**Önerilen yaklaşım:**
- `Payment` entity'e `idempotencyKey` (unique, nullable) alanı ekle
- `PaymentController.pay`'de `X-Idempotency-Key` header al
- `PaymentManager.pay`'de: key varsa mevcut kaydı bul ve döndür, yoksa normal akış

---

### 6. BCrypt Strength Düşük
**Durum:** Yapılmadı  
**Sorun:** `new BCryptPasswordEncoder()` — default strength 10, minimum 12 önerilir.  
**Dosya:** `SecurityConfig.java:89`  
**Düzeltme:** `new BCryptPasswordEncoder(12)`  
**Not:** Mevcut kullanıcıların hash'leri geçersiz kalmaz — BCrypt kendi strength'ini hash içinde saklar.

---

### 7. CORS Wildcard + Credentials
**Durum:** Yapılmadı  
**Sorun:** `AllowedHeaders: *` ile `AllowCredentials: true` birlikte CORS spec'e aykırı.  
**Dosya:** `SecurityConfig.java:97-98`  
**Düzeltme:**
```java
config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
```

---

### 8. Rate Limiting Yok
**Durum:** Yapılmadı  
**Sorun:** `/api/v1/auth/login` ve `/register` brute-force'a açık.  
**Önerilen yaklaşım:** Spring'de built-in rate limiting yok. Seçenekler:
- **Bucket4j** (in-memory veya Redis) — Spring Boot ile kolay entegrasyon
- **Resilience4j RateLimiter** — zaten bağımlılık varsa tercih edilebilir  
**Scope:** Login endpoint'i için IP bazlı, örn. 5 istek/dakika.

---

### 9. PaymentResponse'ta userId Expose
**Durum:** Yapılmadı  
**Sorun:** User kendi ödemesini görürken `userId` alanı gereksiz — user enumeration riski.  
**Dosya:** `PaymentResponse.java`  
**Düzeltme:** `userId` alanını response'tan kaldır. Admin için gerekiyorsa ayrı DTO oluştur.

---

### 10. Rental Complete'te Audit Log Yok
**Durum:** Yapılmadı  
**Sorun:** ADMIN hangi rental'ı complete ettiğinde log bırakmıyor.  
**Dosya:** `RentalController.java` (complete metodu)  
**Düzeltme:** `@Slf4j` + `log.info("Admin {} completed rental {}", authentication.getName(), id)`  
**Not:** Bu endpoint şu an Authentication inject etmiyor — eklenmesi gerekiyor.

---

## Ortam Notları
- `ADMIN_PASSWORD` env var zorunlu — fallback yok, uygulama başlamaz
- `JWT_SECRET` env var zorunlu, min 32 byte — fallback yok, uygulama başlamaz
- Access token süresi **15 dakika** (önceden 24 saat)
- Refresh token süresi **7 gün**, DB'de `refresh_tokens` tablosu
- Build komutu: `mvn install -DskipTests` (clean bazen IDE lock nedeniyle başarısız olabiliyor)
