# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run Commands

All Maven commands must be run from `veyra-api/`:

```bash
# Build entire project
cd veyra-api && mvn clean install

# Build skipping tests
cd veyra-api && mvn clean install -DskipTests

# Build a specific module
cd veyra-api && mvn clean install -pl veyra-auth -am

# Run the application locally (requires PostgreSQL at localhost:5432)
cd veyra-api && mvn -pl veyra-app spring-boot:run

# Run with Docker (PostgreSQL + app)
cd veyra-api && docker-compose up --build
```

Default server: `http://localhost:8080`  
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Module Architecture

Multi-module Maven project under `veyra-api/`:

| Module | Purpose |
|--------|---------|
| `veyra-core` | Shared: `BaseEntity`, `ApiResponse<T>`, `PageResponse<T>`, exceptions, `ErrorCodes` constants |
| `veyra-auth` | JWT auth: login/register, `JwtAuthenticationFilter`, `SecurityConfig` |
| `veyra-user` | User profile management (separate from auth credentials) |
| `veyra-vehicle` | Brands, models, cars — skeleton only |
| `veyra-rental` | Rental operations — skeleton only |
| `veyra-payment` | Payment simulation — skeleton only |
| `veyra-app` | Spring Boot entry point; aggregates all modules |

**Dependency direction (unidirectional):**  
`veyra-app` → domain modules → `veyra-core` (no cross-domain dependencies)

## Package Layout (per domain module)

```
com.veyra.<module>.<domain>/
├── entity/       # JPA entities (extend BaseEntity)
├── repository/   # Spring Data JPA repositories
├── service/      # Interfaces (contracts — controllers depend on these, not impls)
├── manager/      # Service implementations
├── rules/        # Business rule validation (extracted from managers to keep them clean)
├── controller/   # REST endpoints
├── dto/
│   ├── request/
│   └── response/
├── mapper/       # MapStruct entity ↔ DTO
└── enums/
```

## Key Patterns

### Rules Pattern
Business validation is extracted into `*Rules` classes injected into managers:
```java
// Manager calls rules before business logic
authRules.checkIfEmailAlreadyExists(request.getEmail());
```
New business rules belong in `*Rules`, not in managers.

### Service/Manager Split
Controllers always depend on the **interface** (`AuthService`), never the implementation (`AuthManager`). This enforces DIP.

### ApiResponse Envelope
Every endpoint returns `ApiResponse<T>` from `veyra-core`:
```json
{ "success": true, "status": 200, "message": "...", "data": {...}, "timestamp": "..." }
```
Use `ApiResponse.success(data)` / `ApiResponse.error(message, errorCode, status)`.

### BaseEntity
All domain entities extend `BaseEntity` which provides: `id` (UUID auto-gen), `createdAt`, `updatedAt` (JPA audited), `deleted` (soft delete flag).

### Exception Hierarchy
All exceptions extend `BusinessException` from `veyra-core`:
- `ResourceNotFoundException` → 404
- `AlreadyExistsException` → 409
- `UnauthorizedException` → 401
- `ForbiddenException` → 403

`GlobalExceptionHandler` in `veyra-core` handles all of these automatically. Do not add try/catch in controllers or managers for these types.

### Error Codes
Machine-readable codes live in `veyra-core/.../constants/ErrorCodes.java`. Add new codes there; never use raw strings in exceptions.

## Database & Auth

- **PostgreSQL 17** — credentials via env vars (`SPRING_DATASOURCE_URL/USERNAME/PASSWORD`); defaults to `localhost:5432/veyra_db`
- `ddl-auto: update` in dev; planned `validate` in production
- **Soft delete** — set `deleted = true`, never `DELETE FROM`
- **JWT** — stateless; token carries `email`, `userId`, `role`; secret via `JWT_SECRET` env var
- Two roles: `ADMIN`, `USER` — enforced via `@PreAuthorize`
- `AuthUser` and `User` are separate entities — `AuthUser.userId` is a soft reference (no JPA FK) to `User.id`

## MapStruct & Lombok

Lombok must appear **before** MapStruct in the annotation processor path (already configured in parent POM). Do not change this order.

## Annotation Processor Note (Java 25)

The parent POM configures `maven-compiler-plugin` with special `-parameters` and `--enable-preview` args for Java 25 + Lombok compatibility. Do not override these in child modules.

## CORS

Configured for `localhost:3000` and `localhost:5173` (frontend dev servers).

## API Base Paths

| Path | Module | Auth Required |
|------|---------|--------------|
| `/api/v1/auth/**` | veyra-auth | Public |
| `/api/v1/users/**` | veyra-user | ADMIN only |
| `/api/v1/brands/**` | veyra-vehicle | Planned |
| `/api/v1/cars/**` | veyra-vehicle | Planned |
| `/api/v1/rentals/**` | veyra-rental | Planned |
| `/api/v1/payments/**` | veyra-payment | Planned |
