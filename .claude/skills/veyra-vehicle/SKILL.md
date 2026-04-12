---
name: veyra-vehicle
description: Araç katalog modülü. Dört sub-domain — brand, model, car, image. Brand → CarModel → Car zinciri ile JPA ilişkisi var. CarImage Car'a FK ile bağlı. CarRules cross-module olarak veyra-rental tarafından kullanılır.
---

## Sorumluluk
Marka, model, araç ve araç görsellerini yönetir. Brand altında CarModel, CarModel altında Car, Car altında CarImage bulunur. Araç durumu (`CarStatus`) burada tutulur ve kiralama sırasında `veyra-rental` tarafından güncellenir. Görsel depolama `veyra-core`'un `StorageService` abstraction'ı üzerinden (MinIO dev / R2 prod) yapılır.

## Sub-Domain'ler

```
brand/    — Brand entity, BrandRules, BrandService, BrandController
model/    — CarModel entity, CarModelRules, ...
car/      — Car entity, CarRules, CarService, CarController, CarStatus enum
image/    — CarImage entity, CarImageRules, CarImageService, CarImageController
```

Her sub-domain kendi paket düzenine sahip (entity/repository/service/manager/rules/controller/dto/mapper).

## Entity Zinciri

```
Car → CarModel → Brand
```

JPA ilişkileri **gerçek FK** (soft reference değil — bu modül içinde):
- `Car.model` → `@ManyToOne(fetch=LAZY)` `model_id`
- `CarModel.brand` → `@ManyToOne(fetch=LAZY)` `brand_id`

## Brand

| Alan | Tip | Notlar |
|------|-----|--------|
| `name` | `String` | Unique (case-insensitive) |

**Kurallar:**
- Create: `existsByNameIgnoreCase(name)` — varsa 409
- Update: `existsByNameIgnoreCaseAndIdNot(name, id)` — kendi kaydı hariç

## CarModel

| Alan | Tip | Notlar |
|------|-----|--------|
| `name` | `String` | Brand içinde unique |
| `brand` | `Brand` | `@ManyToOne` |

**Kurallar:**
- Aynı brand altında aynı model adı olamaz: `existsByNameIgnoreCaseAndBrandId(name, brandId)`
- Update: `existsByNameIgnoreCaseAndBrandIdAndIdNot(name, brandId, id)`
- Brand silinirse modeller orphan olur (cascade YOK — soft delete kullanıldığı için)

## Car

| Alan | Tip | Notlar |
|------|-----|--------|
| `model` | `CarModel` | `@ManyToOne` |
| `year` | `int` | `@Min(2000)` |
| `doors` | `int` | `@Min(2)` |
| `baggages` | `int` | `@Min(0)` |
| `dailyPrice` | `BigDecimal(10,2)` | `@Positive` |
| `status` | `CarStatus` | `@Enumerated(STRING)`, default `AVAILABLE` |

**Plaka YOK, renk YOK** — bilinçli karar.

## CarStatus Enum

| Değer | Açıklama |
|-------|---------|
| `AVAILABLE` | Kiralanabilir |
| `RENTED` | Kirada |
| `MAINTENANCE` | Bakımda |

## CarRules (cross-module)
`veyra-rental.RentalManager` tarafından kullanılır.

| Metot | Davranış |
|-------|---------|
| `getByIdOrThrow(id)` | Yoksa `CAR_NOT_FOUND` (404) |
| `getByIdOrThrowForUpdate(id)` | Pessimistic lock ile fetch — kiralama sırasında race condition'ı önler |
| `checkIfCarAvailable(car)` | `AVAILABLE` değilse `BusinessRuleException` (`CAR_NOT_AVAILABLE`, 422) |
| `checkIfCarCanBeDeleted(car)` | `RENTED` ise `BusinessRuleException` (`CAR_NOT_AVAILABLE`, 422) — kirada olan araç silinemez |

## CarService (cross-module)
`veyra-rental.RentalManager` car status değişiklikleri için `CarService` interface'ini kullanır (DIP).

| Metot | Davranış |
|-------|---------|
| `markAsRented(carId)` | `getByIdOrThrow` → status = RENTED → save |
| `markAsAvailable(carId)` | `getByIdOrThrow` → status = AVAILABLE → save |

## Endpoint'ler

### Brand
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/brands` | ADMIN |
| PUT | `/api/v1/brands/{id}` | ADMIN |
| GET | `/api/v1/brands/{id}` | **Public** |
| GET | `/api/v1/brands` | **Public** |
| DELETE | `/api/v1/brands/{id}` | ADMIN |

### CarModel
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/models` | ADMIN |
| PUT | `/api/v1/models/{id}` | ADMIN |
| GET | `/api/v1/models/{id}` | **Public** |
| GET | `/api/v1/models?brandId=X` | **Public** — opsiyonel filter |
| DELETE | `/api/v1/models/{id}` | ADMIN |

### Car
| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/cars` | ADMIN | Yeni araç |
| PUT | `/api/v1/cars/{id}` | ADMIN | Güncelle |
| GET | `/api/v1/cars/{id}` | **Public** | Tek kayıt (rate-limited: PUBLIC bucket) |
| GET | `/api/v1/cars` | **Public** | Tüm araçlar (pageable: `?page=0&size=20`) |
| GET | `/api/v1/cars?available=true` | **Public** | Sadece uygun araçlar (pageable) |
| DELETE | `/api/v1/cars/{id}` | ADMIN | Soft delete — kirada olan araç silinemez |

**Not:** Brand/CarModel GET endpoint'leri de public'tir (rate limit altında). Yazma operasyonları (POST/PUT/DELETE) `@PreAuthorize` + SecurityConfig matcher ile ADMIN'e kısıtlıdır.

`UpdateCarRequest` `status` alanını içerir (status manuel güncelleme — örn. MAINTENANCE'a alma).

## Mapper Düzleştirme
`CarResponse` denormalize:
```
modelId, modelName, brandId, brandName, year, doors, baggages, dailyPrice, status, createdAt,
images: List<CarImageResponse>, primaryImageUrl: String
```
MapStruct `model.brand.name → brandName` gibi nested mapping yapar. `images` ve `primaryImageUrl` alanlarını **MapStruct doldurmaz** — `CarManager` image enrichment helper'ları ile batch query üzerinden doldurur (aşağıya bakın).

## CarImage Sub-Domain

### Entity — `car_images` tablosu

| Alan | Tip | Notlar |
|------|-----|--------|
| `car` | `Car` | `@ManyToOne(fetch=LAZY)`, `car_id` FK |
| `storageKey` | `String` | S3 object key — UNIQUE, max 500 char. URL yok, `StorageService.getPublicUrl()` ile türetilir |
| `contentType` | `String` | MIME type — `image/jpeg`, `image/png`, `image/webp` |
| `sizeBytes` | `long` | Byte cinsinden dosya boyutu |
| `displayOrder` | `int` | 1-indexed — reorder endpoint ile değiştirilebilir |
| `isPrimary` | `boolean` | Kapak görseli — bir araçta max 1 tane |

**Index'ler:**
- `idx_carimage_car(car_id, deleted)`
- `idx_carimage_car_primary(car_id, is_primary, deleted)`
- `idx_carimage_storage_key(storage_key)` — UNIQUE

### CarImageRules — Dosya validasyonu
| Metot | Davranış |
|-------|---------|
| `getByIdOrThrow(id)` | Yoksa `CAR_IMAGE_NOT_FOUND` (404) |
| `getByIdForCarOrThrow(imageId, carId)` | Ownership check — başka aracın görselini düzenlemeye karşı koruma, `IMAGE_NOT_OWNED_BY_CAR` |
| `checkFileNotEmpty(file)` | Boş dosya → `FILE_EMPTY` (422) |
| `checkFileSize(file)` | 5MB üzeri → `FILE_SIZE_EXCEEDED` (422) |
| `checkContentType(file)` | jpeg/png/webp değilse → `FILE_TYPE_INVALID` (422) |
| `hasValidImageMagicBytes(file)` | İlk 12 byte'ı okuyup gerçek format'ı doğrular — content-type spoof koruması. JPEG (`FF D8 FF`), PNG (`89 50 4E 47 0D 0A 1A 0A`), WebP (`RIFF....WEBP`) |
| `checkMaxImagesPerCar(carId)` | 10 üzeri → `CAR_IMAGE_LIMIT_EXCEEDED` (422) |

### Endpoint'ler — `/api/v1/cars/{carId}/images`

| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/cars/{carId}/images` | ADMIN | `multipart/form-data` — tek görsel yükler, ilk görsel otomatik primary olur |
| GET | `/api/v1/cars/{carId}/images` | **Public** | Tüm görseller `displayOrder` ASC sıralı |
| PUT | `/api/v1/cars/{carId}/images/{imageId}/primary` | ADMIN | Kapak görseli seç — eski primary unset edilir |
| PUT | `/api/v1/cars/{carId}/images/reorder` | ADMIN | Bulk reorder — `[{imageId, displayOrder}, ...]` |
| DELETE | `/api/v1/cars/{carId}/images/{imageId}` | ADMIN | Soft delete — DB önce, sonra S3 (S3 hata verse bile DB tutarlı kalır). Primary silinirse en küçük `displayOrder` olan görsel otomatik primary olur |

### Upload akışı (`CarImageManager.upload`)
1. `carRules.getByIdOrThrow(carId)` — araç var mı
2. `checkFileNotEmpty` + `checkFileSize` + `checkContentType` + `hasValidImageMagicBytes`
3. `checkMaxImagesPerCar(carId)` — 10 sınırı
4. `storageService.upload(file, "cars/" + carId)` → `StoredFile` döner
5. `displayOrder = findMaxDisplayOrderByCarId + 1` (gap yok)
6. İlk görselse `isPrimary = true`
7. DB save, `toResponse` döndür

### Delete akışı
1. `getByIdForCarOrThrow` — ownership check
2. Soft delete DB (`deleted = true`, save)
3. S3 delete — hata verirse warning log, DB state korunur
4. Silinen primary ise → kalan en düşük `displayOrder`'lı görsel primary olur

### URL türetme
`CarImageMapper` MapStruct **abstract class** (interface değil) — `StorageService` DI için:
```java
@Mapper(componentModel = "spring")
public abstract class CarImageMapper {
    @Autowired protected StorageService storageService;

    @Mapping(source = "car.id", target = "carId")
    @Mapping(target = "url",
             expression = "java(storageService.getPublicUrl(image.getStorageKey()))")
    public abstract CarImageResponse toResponse(CarImage image);
}
```
Bu sayede URL runtime'da türetilir — CDN/vendor değişimi DB'yi etkilemez.

## CarResponse Image Enrichment (N+1 önlemi)

**Mimari karar:** `Car` entity'sine `@OneToMany List<CarImage>` eklenmedi. Sebep:
- JPA Cartesian explosion riski (`JOIN FETCH` ile duplicate rows)
- Domain bağımsızlığı — car paketi image paketine kuplelenmesin
- Query sayısı kontrolü manager'da kalıyor

### Batch fetch pattern
`CarManager` read akışlarında 2 query çalıştırır:
1. `findAllWithModelAndBrand(...)` — car + model + brand JOIN FETCH
2. `findAllByCarIdIn(carIds)` — tüm görseller tek seferde, `ORDER BY car.id, displayOrder`

Sonra in-memory `Map.groupingBy(img -> img.getCar().getId())` ile car'lara dağıtılır. `img.getCar().getId()` FK sütunu olduğu için lazy proxy ek query atmaz.

### Enrichment helper'ları (`CarManager`)
| Helper | Kullanım |
|--------|---------|
| `enrichWithImages(response)` | Tek araç — `getById`, `update` |
| `enrichListWithImages(responses)` | Liste — `getAll`, `getAvailable`, paginated versions (1 ekstra query, N değil) |
| `findPrimaryUrl(images)` | Primary görselin public URL'i — yoksa `null` |

**`create()`:** Yeni araçta görsel olamayacağı için `Collections.emptyList()` set edilir — 0 query.

### `primaryImageUrl` alanı
List view'lar (frontend card UI) için kısa yol — tüm `images` array'ini dolaşmadan kapak görseli URL'i alınabilir. Primary yoksa `null`.

## CarImageRepository batch query
```java
@Query("""
    SELECT ci FROM CarImage ci
    WHERE ci.car.id IN :carIds
    ORDER BY ci.car.id ASC, ci.displayOrder ASC
""")
List<CarImage> findAllByCarIdIn(@Param("carIds") Collection<Long> carIds);
```

## N+1 Query Çözümü — JOIN FETCH
`CarRepository` tüm liste ve detay sorgularında `JOIN FETCH c.model m JOIN FETCH m.brand` kullanır.
20 araçlık listede 21 query yerine tek query çalışır.

| Repository Metot | Kullanım |
|------------------|---------|
| `findAllWithModelAndBrand()` | `getAll()` liste |
| `findAllWithModelAndBrand(Pageable)` | `getAll(pageable)` paginated |
| `findAllByStatusWithModelAndBrand(status)` | `getAvailable()` |
| `findAllByStatusWithModelAndBrand(status, pageable)` | `getAvailable(pageable)` |
| `findByIdWithModelAndBrand(id)` | `getById(id)` |

## Caching
- `BrandManager.getAll()` → `@Cacheable("brands")`
- `CarModelManager.getAll()` → `@Cacheable("models")`
- Create/update/delete → `@CacheEvict(allEntries = true)`

## DB Index'leri
- `cars`: `idx_car_status_deleted(status, deleted)`, `idx_car_model(model_id, deleted)`

## Bağımlılıklar
- `veyra-core`
