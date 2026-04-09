---
name: veyra-vehicle
description: Araç katalog modülü. Üç sub-domain — brand, model, car. Brand → CarModel → Car zinciri ile JPA ilişkisi var. CarRules cross-module olarak veyra-rental tarafından kullanılır.
---

## Sorumluluk
Marka, model ve araç katalogunu yönetir. Brand altında CarModel, CarModel altında Car bulunur. Araç durumu (`CarStatus`) burada tutulur ve kiralama sırasında `veyra-rental` tarafından güncellenir.

## Sub-Domain'ler

```
brand/    — Brand entity, BrandRules, BrandService, BrandController
model/    — CarModel entity, CarModelRules, ...
car/      — Car entity, CarRules, CarService, CarController, CarStatus enum
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
| GET | `/api/v1/brands/{id}` | Authenticated |
| GET | `/api/v1/brands` | Authenticated |
| DELETE | `/api/v1/brands/{id}` | ADMIN |

### CarModel
| Method | Path | Auth |
|--------|------|------|
| POST | `/api/v1/models` | ADMIN |
| PUT | `/api/v1/models/{id}` | ADMIN |
| GET | `/api/v1/models/{id}` | Authenticated |
| GET | `/api/v1/models?brandId=X` | Authenticated — opsiyonel filter |
| DELETE | `/api/v1/models/{id}` | ADMIN |

### Car
| Method | Path | Auth | Açıklama |
|--------|------|------|---------|
| POST | `/api/v1/cars` | ADMIN | Yeni araç |
| PUT | `/api/v1/cars/{id}` | ADMIN | Güncelle |
| GET | `/api/v1/cars/{id}` | Authenticated | Tek kayıt |
| GET | `/api/v1/cars` | Authenticated | Tüm araçlar (pageable: `?page=0&size=20`) |
| GET | `/api/v1/cars?available=true` | Authenticated | Sadece uygun araçlar (pageable) |
| DELETE | `/api/v1/cars/{id}` | ADMIN | Soft delete — kirada olan araç silinemez |

`UpdateCarRequest` `status` alanını içerir (status manuel güncelleme — örn. MAINTENANCE'a alma).

## Mapper Düzleştirme
`CarResponse` denormalize:
```
modelId, modelName, brandId, brandName, year, doors, baggages, dailyPrice, status, createdAt
```
MapStruct `model.brand.name → brandName` gibi nested mapping yapar.

## Bağımlılıklar
- `veyra-core`
