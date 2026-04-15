# Architecture Map — Veyra RentACar API

## Technology Stack
- **Java 25**, Spring Boot 4.0.3, Maven multi-module
- **Database**: PostgreSQL 17 via Spring Data JPA / Hibernate
- **Auth**: Stateless JWT (HS256, JJWT 0.12.6), BCrypt strength 12
- **Storage**: S3-compatible (MinIO dev, Cloudflare R2 prod), AWS SDK v2.30.0
- **API Docs**: SpringDoc OpenAPI 2.8.6 (Swagger UI)
- **Caching**: Spring Cache (Brand/CarModel lists)
- **Virtual Threads**: Enabled (Java 25)

## Detected Languages
- Java (100% of codebase) → activates sc-lang-java

## Application Type
- REST API (Spring Boot multi-module monolith)

## Modules
| Module | Responsibility |
|--------|---------------|
| veyra-core | BaseEntity, exceptions, ErrorCodes, GlobalExceptionHandler, S3StorageService |
| veyra-auth | JWT lifecycle, SecurityConfig, JwtAuthenticationFilter, RateLimitFilter, AuthUser, refresh tokens |
| veyra-user | User profile CRUD (separate from auth credentials) |
| veyra-vehicle | Brand, CarModel, Car, CarImage with S3 storage + magic byte validation |
| veyra-rental | Rental create/complete/cancel workflows |
| veyra-payment | Payment simulation with idempotency key |
| veyra-app | Entry point, SwaggerConfig, aggregates all modules |

## Entry Points (9 Controllers, ~30 endpoints)

### Public (No Auth)
- POST `/api/v1/auth/register`, `/login`, `/refresh`, `/logout`
- GET `/api/v1/cars/**`, `/brands/**`, `/models/**`
- GET `/swagger-ui/**`, `/v3/api-docs/**`
- GET `/actuator/health`

### Authenticated (USER or ADMIN)
- POST `/api/v1/rentals`, `/rentals/{id}/cancel`
- GET `/api/v1/rentals/my`, `/rentals/{id}`
- POST `/api/v1/payments`
- GET `/api/v1/payments/my`, `/payments/{id}`

### Admin Only
- PUT `/api/v1/admin/users/{userId}/role`
- ALL `/api/v1/users/**`
- POST/PUT/DELETE on `/brands/**`, `/models/**`, `/cars/**`, `/cars/{carId}/images/**`
- POST `/api/v1/rentals/{id}/complete`
- GET `/api/v1/rentals` (all), `/api/v1/payments` (all)
- All DELETE requests globally require ADMIN

## Authentication Architecture
- **JWT**: HS256, 15 min access, 7 day refresh (DB-backed UUID)
- **Secret**: env var JWT_SECRET, min 32 bytes, validated at startup
- **Claims**: sub (email), userId, role, iat, exp
- **Refresh**: Token rotation (old deleted, new created on each refresh)
- **Password**: BCrypt strength 12, complexity regex enforced
- **Admin Seeder**: Creates admin from ADMIN_EMAIL/ADMIN_PASSWORD env vars at startup

## Security Controls
| Control | Status |
|---------|--------|
| CSRF | Disabled (stateless JWT API — appropriate) |
| CORS | Restrictive origin whitelist from env var |
| Rate Limiting | 5 req/60s auth, 60 req/60s public catalog, IP-based |
| Input Validation | Jakarta Bean Validation + file magic bytes |
| SQL Injection | JPA parameterized queries only |
| Authorization | @PreAuthorize + SecurityConfig matchers + service-layer ownership checks |
| Error Handling | GlobalExceptionHandler, no stack traces exposed |
| File Upload | 5MB limit, MIME whitelist + magic byte validation, UUID keys |
| X-Forwarded-For | Trusted proxy validation (rightmost-untrusted algorithm) |
| Actuator | Only health + info exposed, details require auth |

## External Integrations
- PostgreSQL (HikariCP: 20 max, 5 min idle)
- S3-compatible storage (MinIO/R2/AWS)
- No outbound HTTP calls to third-party APIs
- No message queues, no WebSockets

## Configuration
- All secrets externalized to environment variables
- No hardcoded credentials in code
- spring-dotenv for local dev .env loading
- Swagger controllable via SWAGGER_ENABLED env var
