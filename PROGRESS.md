# Veyra API — Geliştirme Günlüğü

Bu dosya projede **yapılanları** ve **sıradaki işleri** kronolojik / faz bazlı takip eder. Her yeni çalışma başında buradan devam edilir.

---

## TAMAMLANAN ÇALIŞMALAR

### Önceki Mimari İyileştirme Planı (plan: `velvet-spinning-pillow.md`)

| Faz | Başlık | Durum |
|-----|--------|-------|
| 1 | `getByIdOrThrow` pattern — UserRules + AuthRules | TAMAM |
| 2 | CarService status encapsulation (DIP) | TAMAM |
| 3 | Ownership check DRY — `SecurityUtils.checkOwnership` | TAMAM |
| 4 | User silme cascade — Spring `UserDeletedEvent` | TAMAM |
| 5 | User update endpoint (`PUT /users/{id}` — ADMIN) | TAMAM |
| 6 | Pagination — cars/rentals/payments/users | TAMAM |
| 7 | Cascading soft delete koruması — RENTED araç silinemez | TAMAM |

---

### Car Image Feature — S3-compatible object storage

Rent-a-car için kritik eksikti. Dev'de **MinIO** (self-hosted, ücretsiz), prod'da **Cloudflare R2** (10GB free + unlimited egress). Tek implementasyon, sadece env var'lar değişir.

#### Faz 1 — Infrastructure ✅
- `veyra-api/pom.xml` — AWS SDK v2 BOM (`software.amazon.awssdk:bom:2.30.0`)
- `veyra-core/pom.xml` — `s3` dependency
- `docker-compose.yml` — MinIO service (port 9000 API, 9001 console, healthcheck, volume)
- `.env` + `.env.example` — 8 storage env var
- `application.yml` — `storage.s3` config bloğu
- Build doğrulandı ✅

#### Faz 2 — Storage Abstraction (`veyra-core/storage/`) ✅
- `StorageService` interface — `upload`, `delete`, `getPublicUrl`
- `StoredFile` record — **URL YOK** (bilinçli karar: vendor/CDN/signed URL'e geçiş riski)
- `StorageProperties` — `@ConfigurationProperties("storage.s3")` record, 8 alan
- `S3StorageConfig` — `S3Client` bean + `@EventListener(ApplicationReadyEvent.class)` bucket bootstrap
- `S3StorageService` — UUID filename + regex-sanitized extension (path traversal koruması)
- `StorageException` — `BusinessException` subclass, HTTP 500
- Build doğrulandı ✅

#### Faz 3 — CarImage Domain (`veyra-vehicle/image/`) ✅
- `ErrorCodes`: `FILE_EMPTY`, `FILE_TYPE_INVALID`, `FILE_SIZE_EXCEEDED`, `CAR_IMAGE_NOT_FOUND`, `CAR_IMAGE_LIMIT_EXCEEDED`, `IMAGE_NOT_OWNED_BY_CAR`
- `CarImage` entity — `car_images` tablosu, 3 index (car FK, primary, unique storage_key), `@SQLRestriction`
- `CarImageRepository` — find/count/max-order/ownership query'leri
- `CarImageRules` — file validation + **magic byte kontrolü** (JPEG/PNG/WebP), 5MB sınır, araç başına 10 görsel
- DTO'lar — `CarImageResponse`, `ReorderImagesRequest`, `ReorderImageItem`
- `CarImageMapper` — MapStruct **abstract class** (interface değil) — `StorageService` DI için, URL runtime'da türetilir
- `CarImageService` interface
- `CarImageManager`:
  - Upload: car exists → file validation → 10 limit → S3 upload → displayOrder = max+1 → ilk görsel auto-primary → DB save
  - Delete: soft delete DB → S3 delete (hata verirse warning log) → primary silinirse en düşük displayOrder otomatik primary olur
  - setPrimary / reorder / getByCarId
- `CarImageController` — 5 endpoint:
  - `POST /api/v1/cars/{carId}/images` (ADMIN, `multipart/form-data`)
  - `GET /api/v1/cars/{carId}/images` (**Public**)
  - `PUT /api/v1/cars/{carId}/images/{imageId}/primary` (ADMIN)
  - `PUT /api/v1/cars/{carId}/images/reorder` (ADMIN)
  - `DELETE /api/v1/cars/{carId}/images/{imageId}` (ADMIN)
- Build doğrulandı ✅

#### Faz 3.5 — Security + Rate Limiting (mid-faz düzeltme) ✅
**Tetik:** Kullanıcı `GET /cars/{carId}/images` authenticated olmasının yanlış olduğunu fark etti. Rent-a-car ziyaretçileri giriş yapmadan araç görsellerini görebilmeli.

- `RateLimitFilter` — Bucket sistemine yeniden yazıldı:
  - `AUTH` bucket: `/api/v1/auth/**` — 5 req / 60 sn (brute-force koruması)
  - `PUBLIC` bucket: GET `/cars|/brands|/models/**` — 60 req / 60 sn (DDoS/scraping koruması)
  - `NONE`: Authenticated endpoint'ler — limit yok (JWT zaten kimlik doğruluyor)
  - Key format: `{ip}:{bucket}` — aynı IP iki bucket'ta ayrı sayılır
  - `X-Forwarded-For` sadece `TRUSTED_PROXIES`'ten parse edilir (rightmost-untrusted)
- `SecurityConfig` — Public GET kuralları eklendi (`cars`, `brands`, `models`)
- Build doğrulandı ✅

#### Faz 4 — CarResponse + CarManager Entegrasyonu ✅
**Mimari karar:** `Car` entity'sine `@OneToMany` eklenmedi. Sebep: JPA Cartesian explosion, domain kuplelenme, query kontrolü kaybı.

- `CarImageRepository.findAllByCarIdIn(carIds)` — batch query, `ORDER BY car.id, displayOrder`
- `CarResponse`:
  - `@Setter` eklendi (manager enrichment için)
  - `List<CarImageResponse> images` alanı
  - `String primaryImageUrl` alanı (list view card UI için kısa yol)
- `CarManager` rewrite:
  - `CarImageRepository` + `CarImageMapper` inject
  - `enrichWithImages(response)` — tek araç, 1 ekstra query
  - `enrichListWithImages(responses)` — liste, 1 ekstra query (N değil)
  - `findPrimaryUrl(images)` — primary görselin URL'i veya null
  - `create()`: empty list (0 query)
  - `getById` / `update` → `enrichWithImages`
  - `getAll` / `getAvailable` / paginated versions → `enrichListWithImages`
- Build doğrulandı ✅ — 8/8 modül SUCCESS

#### Faz 5 — Dokümantasyon ✅
- `.claude/skills/veyra-core/SKILL.md` — Storage Abstraction bölümü
- `.claude/skills/veyra-vehicle/SKILL.md` — CarImage sub-domain, CarResponse enrichment, public GET'ler
- `.claude/skills/veyra-app/SKILL.md` — Storage config, MinIO docker-compose
- `CLAUDE.md` — Public catalog kuralı, rate limit bucket sistemi, storage env var listesi, MinIO console
- `PROGRESS.md` — Bu dosya

---

### User Self-Deletion (`DELETE /api/v1/users/me`) ✅

**Sorun:** `SecurityConfig`'teki `DELETE /**` → ADMIN kuralı kullanıcının kendi hesabını silmesini engelliyordu.

**Çözüm:**
- `UserService.deleteByEmail(String email)` interface metodu eklendi
- `UserManager`:
  - `doDelete(User)` private helper — DRY; hem admin-by-id hem self-by-email aynı akışı çalıştırır
  - `delete(id)` → `doDelete(userRules.getByIdOrThrow(id))`
  - `deleteByEmail(email)` → `doDelete(userRules.getByEmailOrThrow(email))`
  - Her iki yol da `UserDeletedEvent` yayınlar → `UserDeletedEventListener` AuthUser'ı soft-delete eder + refresh token'ları revoke eder
- `UserController.deleteSelf(Authentication)` — `/me` endpoint, JWT'den `authentication.getName()` ile email alır (id spoofing yok)
- `SecurityConfig` — `DELETE /api/v1/users/me` matcher'ı `DELETE /**` kuralından **önce** eklendi:
  ```java
  .requestMatchers(HttpMethod.DELETE, ApiConstants.USERS + "/me").authenticated()
  .requestMatchers(HttpMethod.DELETE, "/**").hasRole("ADMIN")
  ```
  Spring Security ilk eşleşen kazanır — dar kural önce, geniş güvenlik ağı sonra.
- Build doğrulandı ✅

**Kapsam dışı (bilinçli ertelendi):**
- Son admin koruması (admin hesabı kendini silerse kontrol)
- Aktif kiralaması olan kullanıcının silinmesi (orphan rental riski)

---

## ÖNCELİK SIRASI — SIRADAKİ İŞLER

### 1. Son Admin Koruması 🟡
Admin hesabı `/me` endpoint'i üzerinden kendini silebiliyor — şu an engelleme yok. `AdminSeeder` startup'ta geri oluştursa da "çalışırken silme" riski var.

**Çözüm yönleri:**
- `UserRules.checkIfNotLastAdmin(user)` — role bilgisi `AuthUser`'da olduğu için cross-module query gerekir. `veyra-user → veyra-auth` bağımlılık yönünü bozar.
- Alternatif: Event'ten önce bir **pre-delete check** — `veyra-auth` tarafında `AuthUserRepository.countByRoleAndDeletedFalse("ADMIN") > 1` kontrolü. Ama bu kontrol `UserManager.delete` çağrısından önce yapılmalı → event kullanılamaz.
- En temiz: `veyra-auth`'ta bir `AdminGuardService` → `UserController.deleteSelf` çağrısından önce bir check → son admin ise reddet. `veyra-user` bu servisi bilmemeli; `veyra-app` seviyesinde orchestrate edilebilir ya da `veyra-auth`'tan bir `@EventListener(UserDeletionRequestedEvent)` ile pre-check yapılabilir.

### 2. Aktif Kiralaması Olan Kullanıcının Silinmesi 🟡
`UserManager.delete` aktif rental kontrolü yapmıyor. Kullanıcı kendi hesabını silerse:
- Araç `RENTED` statüsünde orphan kalabilir (rental.userId soft-deleted user'a işaret eder)
- Ödemesi yapılmamış rental → borç kaçırma vektörü

**Çözüm yönleri:**
- Bağımlılık yönü problemi: `veyra-user → veyra-rental` yasak (mevcut: `veyra-rental → veyra-user`)
- Seçenek A: Pre-delete event (`UserDeletionRequestedEvent`) + rental listener aktif rental varsa runtime exception → transaction rollback
- Seçenek B: `veyra-app` seviyesinde `AccountService` orchestrator — birden fazla modülü koordine eder
- Pragmatik C: Kullanıcı kendi hesabını silmeden önce rental'ları iptal etmesi gerektiğini frontend'de göster; backend 422 döner

### 3. Görsel Yükleme — Real-world Test
- MinIO docker başlatıp gerçek image upload → S3 → URL dönüş akışını manuel test et
- Magic byte spoofing test (jpeg uzantılı exe reddedilmeli)
- 10 görsel sınırı, primary reassign, reorder edge case'leri
- İleride: integration test yazılması (`@Testcontainers` ile MinIO)

### 4. Cloudflare R2 Production Setup (deploy zamanı)
- R2 bucket create, API token üret
- Prod env var'ları: `STORAGE_S3_ENDPOINT` (R2 endpoint), `STORAGE_S3_PUBLIC_BASE_URL` (R2 custom domain / public dev URL)
- `STORAGE_S3_AUTO_CREATE_BUCKET=false` (prod'da elle yönetilsin)
- CORS policy R2'de yapılandırılmalı (frontend'den direkt GET için)

### 5. CarImage Manual Tests + Documentation Sample
- Postman / HTTP client collection'ına `multipart/form-data` örneği
- Swagger'da `@Operation` ve `@RequestBody(content = @Content(mediaType = MULTIPART_FORM_DATA))` annotations'ın örnek response'u göstermesi

### 6. Frontend Entegrasyon (opsiyonel — kapsam dışı olabilir)
- Drag-drop upload component
- `primaryImageUrl` ile card view
- Reorder UI (bulk PUT endpoint hazır)

### 7. İleride Değerlendirilecek
- **Signed URL'ler** — private görseller için (şu an hepsi public). `getPublicUrl` yerine `getSignedUrl(key, ttl)` eklenebilir.
- **Image resize / thumbnail** — upload sırasında server-side resize (ör. 200x200 thumbnail). Trade-off: CPU yükü vs. bandwidth.
- **CDN entegrasyonu** — R2 önüne Cloudflare CDN (zaten native entegre).
- **Image deletion audit log** — hangi admin hangi görseli sildi.
- **Brand/CarModel cascade soft delete** — şu an orphan modeller çalışmaya devam ediyor, ileride hiyerarşik soft delete düşünülebilir.

---

## MİMARİ NOTLAR (unutulmaması gereken kararlar)

- **Storage'da URL saklanmaz.** Sadece `storageKey`. Vendor/CDN/signed URL geçişi olursa DB etkilenmez.
- **`Car.images` @OneToMany YOK.** Cartesian explosion + domain kuplelenme riskine karşı batch fetch pattern tercih edildi.
- **MapStruct abstract class** — `StorageService` gibi runtime bean'leri için gerekli. Interface olarak kullanılamaz.
- **Magic byte check zorunlu** — content-type header spoof edilebilir, gerçek format ilk byte'larda.
- **Public catalog + rate limit** — authenticated yapmak yerine rate limit ile koruma. UX > sürtünme.
- **Bucket sistemi** — aynı IP'nin auth bucket'ı dolunca catalog browse'u etkilenmesin.
- **Soft delete DB önce, S3 sonra** — S3 hata verirse DB tutarlı kalır, retry mümkün.
- **`getByIdOrThrow` pattern** — Manager asla `repository.findById().orElseThrow()` yazmaz, Rules'a devreder.

---

## BUILD DURUMU

Son doğrulama: **BUILD SUCCESS** — 8/8 modül (veyra-core, veyra-auth, veyra-user, veyra-vehicle, veyra-rental, veyra-payment, veyra-app).
