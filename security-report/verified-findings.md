# Verified Security Findings

## Summary
- Total raw findings from Phase 2: 23
- After duplicate merging: 19
- After false positive elimination: 16
- Final verified findings: 16

## Confidence Distribution
- Confirmed (90-100): 2
- High Probability (70-89): 5
- Probable (50-69): 4
- Possible (30-49): 3
- Low Confidence (0-29): 2

---

## Verified Findings

### VULN-001: Weak Admin Password Without Strength Validation
- **Severity:** High
- **Confidence:** 90/100 (Confirmed)
- **Original Skill:** sc-auth, sc-lang-java
- **Vulnerability Type:** CWE-521 (Weak Password Requirements)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/config/AdminSeeder.java:37-42`
- **Reachability:** Direct (runs on every app startup)
- **Sanitization:** None
- **Framework Protection:** None
- **Description:** AdminSeeder accepts any non-blank password from the ADMIN_PASSWORD env var. Regular users must meet strict requirements (10-128 chars, uppercase, lowercase, digit, special char) via RegisterRequest validation. But the admin account — the highest-privilege account — has NO password strength validation. The actual .env file contains `ADMIN_PASSWORD=ss12ss12!` which is only 9 characters and fails the 10-char minimum enforced for regular users.
- **Verification Notes:** Confirmed by reading AdminSeeder.java (line 38: only checks null/blank) and .env (line 15: `ss12ss12!`). The RegisterRequest.java enforces `@Size(min=10)` + `@Pattern` but AdminSeeder bypasses this entirely.
- **Remediation:** Add password strength validation in AdminSeeder matching RegisterRequest rules. Change the admin password immediately to a strong one.

### VULN-002: No Account Lockout After Failed Login Attempts
- **Severity:** High
- **Confidence:** 85/100 (High Probability)
- **Original Skill:** sc-auth, sc-rate-limiting
- **Vulnerability Type:** CWE-307 (Improper Restriction of Excessive Authentication Attempts)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/manager/AuthManager.java:88-94`
- **Reachability:** Direct (login endpoint)
- **Sanitization:** Rate limit provides partial mitigation (5 req/60s per IP)
- **Framework Protection:** None
- **Description:** Login endpoint has no account lockout mechanism. While RateLimitFilter limits to 5 requests per 60 seconds per IP, an attacker using distributed IPs (botnet) can bypass this entirely. There's no per-account tracking of failed attempts — no temporary lock, no CAPTCHA escalation, no exponential backoff.
- **Verification Notes:** Confirmed by reading AuthManager.login() — only throws UnauthorizedException on wrong password, no failed attempt counter. RateLimitFilter uses IP-based rate limiting which is bypassable with multiple IPs.
- **Remediation:** Implement per-account failed login tracking. After 5 consecutive failures, lock the account for 15 minutes. Add CAPTCHA after 3 failures. Log all failed attempts with IP and timestamp.

### VULN-003: User Enumeration via Registration and Login Error Messages
- **Severity:** Medium
- **Confidence:** 85/100 (High Probability)
- **Original Skill:** sc-auth, sc-data-exposure
- **Vulnerability Type:** CWE-204 (Observable Response Discrepancy)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/rules/AuthRules.java:27-34`
- **Reachability:** Direct (public endpoints)
- **Sanitization:** None
- **Framework Protection:** None
- **Description:** Registration endpoint returns `"Bu e-posta adresi zaten kayıtlı: {email}"` (409 status) when an email exists, while login returns `"E-posta veya şifre hatalı"` (generic 401). The registration endpoint confirms email existence — an attacker can enumerate valid accounts by attempting registration with target emails.
- **Verification Notes:** AuthRules.checkIfEmailAlreadyExists() at line 29 confirms the email back to the caller. The login error message is properly generic, but registration leaks account existence.
- **Remediation:** Return a generic message for registration: "Kayıt işlemi tamamlandı. E-posta adresinizi kontrol edin." regardless of whether the email already exists. Send a notification email to existing users instead.

### VULN-004: Missing Security Response Headers
- **Severity:** Medium
- **Confidence:** 90/100 (Confirmed)
- **Original Skill:** sc-clickjacking, sc-api-security, sc-lang-java
- **Vulnerability Type:** CWE-693 (Protection Mechanism Failure)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/config/SecurityConfig.java`
- **Reachability:** Direct (all responses)
- **Sanitization:** N/A
- **Framework Protection:** Spring Security provides headers but they are not explicitly configured
- **Description:** No security response headers are configured: X-Frame-Options, X-Content-Type-Options, Strict-Transport-Security (HSTS), Content-Security-Policy, X-XSS-Protection, Referrer-Policy, Permissions-Policy. While Spring Security adds some defaults, explicit configuration is missing and the impact depends on Spring Boot 4.0.3 defaults.
- **Verification Notes:** Grep for all security header names returned zero matches in application code. SecurityConfig does not call `.headers()` configuration.
- **Remediation:** Add explicit security headers in SecurityConfig:
  ```java
  .headers(headers -> headers
      .frameOptions(f -> f.deny())
      .contentTypeOptions(Customizer.withDefaults())
      .httpStrictTransportSecurity(h -> h.maxAgeInSeconds(31536000).includeSubDomains(true))
  )
  ```

### VULN-005: Swagger UI Enabled by Default in Production
- **Severity:** Medium
- **Confidence:** 75/100 (High Probability)
- **Original Skill:** sc-api-security, sc-data-exposure
- **Vulnerability Type:** CWE-200 (Exposure of Sensitive Information)
- **File:** `veyra-app/src/main/resources/application.yml:47-52`
- **Reachability:** Direct (public endpoint)
- **Sanitization:** None
- **Framework Protection:** None
- **Description:** Swagger UI defaults to enabled (`SWAGGER_ENABLED:true`). If the SWAGGER_ENABLED env var is not explicitly set to `false` in production, the full API documentation including all endpoints, request/response schemas, and authentication details will be publicly accessible. SecurityConfig permits `/swagger-ui/**` and `/v3/api-docs/**` without authentication.
- **Verification Notes:** application.yml line 47: `enabled: ${SWAGGER_ENABLED:true}`. SecurityConfig line 52-54 permits Swagger paths. This is a configuration issue that depends on production deployment practices.
- **Remediation:** Change default to `false`: `enabled: ${SWAGGER_ENABLED:false}`. Or use Spring profiles to disable in prod: `spring.profiles.active=prod` with profile-specific config.

### VULN-006: No Rate Limiting on Authenticated Endpoints
- **Severity:** Medium
- **Confidence:** 70/100 (High Probability)
- **Original Skill:** sc-rate-limiting, sc-api-security
- **Vulnerability Type:** CWE-770 (Allocation of Resources Without Limits)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/filter/RateLimitFilter.java:49`
- **Reachability:** Direct
- **Sanitization:** None
- **Framework Protection:** None
- **Description:** Authenticated endpoints (rentals, payments, user management) have NO rate limiting (Bucket.NONE). A compromised or malicious JWT holder can send unlimited requests. While JWT identifies the user, there's no protection against resource exhaustion, data scraping, or automated abuse by authenticated users.
- **Verification Notes:** RateLimitFilter.resolveBucket() returns Bucket.NONE for all non-auth, non-public-GET paths. shouldNotFilter() returns true for NONE bucket, so the filter is completely skipped.
- **Remediation:** Add a per-user rate limit bucket for authenticated endpoints (e.g., 120 req/min per userId). Extract userId from JWT claims in the filter.

### VULN-007: Race Condition on Payment Idempotency Key Check
- **Severity:** Medium
- **Confidence:** 65/100 (Probable)
- **Original Skill:** sc-race-condition, sc-business-logic
- **Vulnerability Type:** CWE-362 (Race Condition / TOCTOU)
- **File:** `veyra-payment/src/main/java/com/veyra/payment/manager/PaymentManager.java:35-39`
- **Reachability:** Direct (authenticated endpoint)
- **Sanitization:** @Transactional present
- **Framework Protection:** Partial (transaction isolation)
- **Description:** The idempotency key check (`findByIdempotencyKey`) and subsequent payment creation are not atomically protected against concurrent requests with the same key. Two simultaneous requests with the same idempotency key could both pass the `existing.isPresent()` check and create duplicate payments. Default PostgreSQL transaction isolation (READ COMMITTED) does not prevent this.
- **Verification Notes:** PaymentManager.pay() line 36-39 does a read-then-write without pessimistic locking. While @Transactional is present, READ COMMITTED isolation allows both transactions to read "no existing" before either commits.
- **Remediation:** Add a unique constraint on `idempotency_key` column in the payments table. The database will reject the duplicate insert, and the application can catch the constraint violation and return the existing payment.

### VULN-008: JWT Access Tokens Cannot Be Revoked
- **Severity:** Medium
- **Confidence:** 60/100 (Probable)
- **Original Skill:** sc-jwt, sc-session
- **Vulnerability Type:** CWE-613 (Insufficient Session Expiration)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/token/JwtServiceImpl.java:69-72`
- **Reachability:** Direct
- **Sanitization:** 15-minute expiration provides mitigation
- **Framework Protection:** None
- **Description:** JWT access tokens are stateless and cannot be revoked before expiration. If a token is compromised, the attacker has a 15-minute window of unrestricted access. Logout only revokes the refresh token but the access token remains valid. Changing a user's role also doesn't invalidate existing access tokens — a demoted admin retains admin access until token expiry.
- **Verification Notes:** JwtServiceImpl.isTokenValid() only checks expiration and username match — no blacklist check. AuthManager.logout() only calls refreshTokenService.revokeByToken(). AuthManager.changeRole() doesn't invalidate existing JWTs.
- **Remediation:** For critical actions (role change, password change, user delete), implement a short-lived JWT blacklist (Redis with 15-min TTL). Or reduce access token lifetime to 5 minutes for sensitive deployments.

### VULN-009: Unbounded Pageable Size Parameter
- **Severity:** Medium
- **Confidence:** 60/100 (Probable)
- **Original Skill:** sc-api-security, sc-rate-limiting
- **Vulnerability Type:** CWE-400 (Uncontrolled Resource Consumption)
- **File:** Multiple controllers (UserController, RentalController, PaymentController, CarController)
- **Reachability:** Direct
- **Sanitization:** None (Spring Boot 4.0.3 may have default max-page-size)
- **Framework Protection:** Partial (Spring Boot may enforce `spring.data.web.pageable.max-page-size` default of 2000)
- **Description:** All paginated endpoints use `@PageableDefault(size = 20)` but don't explicitly limit the maximum page size. A client can send `?size=999999` to fetch enormous result sets, causing memory pressure and slow queries. While Spring Boot may enforce a default max-page-size of 2000, this is not explicitly configured.
- **Verification Notes:** No `max-page-size` configuration found in application.yml. @PageableDefault only sets the default, not the maximum. Spring Boot 4.0.3 may have a default max of 2000.
- **Remediation:** Add to application.yml: `spring.data.web.pageable.max-page-size: 100`

### VULN-010: Docker Services Expose Ports to Host
- **Severity:** Low
- **Confidence:** 50/100 (Probable)
- **Original Skill:** sc-docker
- **Vulnerability Type:** CWE-284 (Improper Access Control)
- **File:** `docker-compose.yml:13-14, 29-30`
- **Reachability:** Network-dependent (development setup)
- **Sanitization:** N/A
- **Framework Protection:** N/A
- **Description:** PostgreSQL (5432), MinIO S3 API (9000), and MinIO Console (9001) are all exposed to the host network. In a production Docker deployment, these should only be accessible within the Docker network. PostgreSQL with the configured password (`veyra_pass`) would be directly accessible.
- **Verification Notes:** docker-compose.yml is marked as dev configuration. Production would use different deployment. However, if this file is used in production, the database would be network-accessible.
- **Remediation:** For production docker-compose, remove port mappings and only expose via Docker internal network. Or bind to localhost only: `"127.0.0.1:5432:5432"`.

### VULN-011: Hardcoded Healthcheck Username in Docker Compose
- **Severity:** Low
- **Confidence:** 70/100 (High Probability)
- **Original Skill:** sc-docker, sc-secrets
- **Vulnerability Type:** CWE-798 (Use of Hard-coded Credentials)
- **File:** `docker-compose.yml:17`
- **Reachability:** Indirect
- **Sanitization:** N/A
- **Framework Protection:** N/A
- **Description:** The PostgreSQL healthcheck uses hardcoded values: `pg_isready -U veyra_user -d veyra_db`. If the `POSTGRES_USER` or `POSTGRES_DB` env vars are changed, the healthcheck will fail silently, causing Docker to report the container as unhealthy. The username should reference the env var.
- **Verification Notes:** docker-compose.yml line 17 uses literal `veyra_user` and `veyra_db` instead of `${POSTGRES_USER}` and `${POSTGRES_DB}`.
- **Remediation:** Use env var references: `pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}`

### VULN-012: .env File Contains Real Credentials in Working Directory
- **Severity:** Low
- **Confidence:** 50/100 (Probable — context-dependent)
- **Original Skill:** sc-secrets
- **Vulnerability Type:** CWE-256 (Plaintext Storage of a Password)
- **File:** `.env:11,15`
- **Reachability:** Local filesystem
- **Sanitization:** .gitignore prevents commit
- **Framework Protection:** N/A
- **Description:** The `.env` file contains actual credentials: JWT_SECRET (`spV+EUsuxGBaP6YS3AW/0fkFLuB5uhGte8TCGKlUtHs=`), admin password (`ss12ss12!`), DB password (`veyra_pass`), MinIO credentials (`minioadmin/minioadmin123`). While `.gitignore` properly excludes `.env`, and git status confirms it's not tracked, the file exists locally with weak dev credentials. If the same file is used in production without changing values, all credentials would be compromised.
- **Verification Notes:** .gitignore line 19 excludes `.env`. Git status shows `.env` is not tracked. These appear to be dev-only credentials. However, ADMIN_PASSWORD=ss12ss12! is notably weak.
- **Remediation:** Ensure production uses strong, unique credentials via a secrets manager (Vault, AWS Secrets Manager). Never copy .env to production servers.

### VULN-013: No Audit Logging for Security Events
- **Severity:** Low
- **Confidence:** 55/100 (Probable)
- **Original Skill:** sc-auth, sc-api-security
- **Vulnerability Type:** CWE-778 (Insufficient Logging)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/manager/AuthManager.java`
- **Reachability:** N/A (missing feature)
- **Sanitization:** N/A
- **Framework Protection:** None
- **Description:** Critical security events are not logged: failed login attempts (only exception thrown, not logged), successful logins, registration, token refresh, role changes, user deletions. Only rental completion has an explicit log (RentalController line 49). Without security audit logs, incident detection and forensic analysis are impossible.
- **Verification Notes:** AuthManager has no log statements. Only RentalController.complete() logs admin action. No structured security event logging exists.
- **Remediation:** Add structured audit logging for: login success/failure (with IP), registration, logout, role changes, user deletion, admin actions. Use a dedicated security logger with structured format (JSON).

### VULN-014: Login Revokes All Previous Sessions
- **Severity:** Low
- **Confidence:** 45/100 (Possible)
- **Original Skill:** sc-session, sc-business-logic
- **Vulnerability Type:** CWE-613 (Insufficient Session Expiration)
- **File:** `veyra-auth/src/main/java/com/veyra/auth/manager/AuthManager.java:97`
- **Reachability:** Direct
- **Sanitization:** N/A
- **Framework Protection:** N/A
- **Description:** Every login call executes `refreshTokenService.revokeAllByAuthUserId()`, invalidating ALL existing refresh tokens for that user. This means logging in from a new device forces logout on all other devices. While this could be intentional (single-session policy), it can be used as a DoS vector: if an attacker knows a user's credentials, they can repeatedly log in to disrupt the legitimate user's sessions.
- **Verification Notes:** AuthManager.login() line 97 deletes all refresh tokens before creating a new one. Combined with VULN-002 (no account lockout), an attacker with valid credentials can continuously disrupt the account.
- **Remediation:** Consider allowing multiple concurrent sessions (don't revoke on login). Or add session management UI where users can view/revoke individual sessions.

### VULN-015: DDL Auto-Update Enabled by Default
- **Severity:** Low
- **Confidence:** 40/100 (Possible)
- **Original Skill:** sc-lang-java, sc-iac
- **Vulnerability Type:** CWE-1188 (Insecure Default Initialization)
- **File:** `veyra-app/src/main/resources/application.yml:22`
- **Reachability:** Application startup
- **Sanitization:** Configurable via DDL_AUTO env var
- **Framework Protection:** None
- **Description:** Hibernate DDL auto defaults to `update` (`${DDL_AUTO:update}`). If DDL_AUTO env var is not set in production, Hibernate will automatically modify the database schema on startup. This can cause data loss, schema corruption, or unauthorized schema changes if a modified entity is deployed.
- **Verification Notes:** application.yml line 22: `ddl-auto: ${DDL_AUTO:update}`. The default should be `validate` for safety.
- **Remediation:** Change default to `validate`: `ddl-auto: ${DDL_AUTO:validate}`. Use Flyway or Liquibase for production migrations.

### VULN-016: Missing Pagination on Legacy List Methods
- **Severity:** Info
- **Confidence:** 30/100 (Low Confidence)
- **Original Skill:** sc-api-security
- **Vulnerability Type:** CWE-400 (Uncontrolled Resource Consumption)
- **File:** `veyra-rental/src/main/java/com/veyra/rental/manager/RentalManager.java:107-112`
- **Reachability:** Indirect (method exists but may not be exposed via controller)
- **Sanitization:** Admin-only access
- **Framework Protection:** None
- **Description:** RentalManager and PaymentManager have `getAll()` methods that return all records without pagination (`findAll()` returns full list). While the controllers use paginated versions, these unpaginated methods exist in the service layer and could be called from future code paths, loading the entire table into memory.
- **Verification Notes:** RentalManager.getAll() at line 107 returns `rentalRepository.findAll()` without pagination. However, the controllers use the paginated overloads. This is a code-level concern, not currently exploitable via API.
- **Remediation:** Remove unpaginated `getAll()` methods or add a hard limit. Mark as @Deprecated if kept for internal use.

---

## Eliminated Findings (False Positives)

1. **SQL Injection via @Query annotations** — All @Query annotations use JPQL with named parameters (`:now`, `:status`, `:id`, `:carId`). No string concatenation. Spring Data JPA auto-parameterizes. **False positive.**

2. **CSRF disabled** — CSRF is appropriately disabled for a stateless JWT-based REST API with no cookie-based authentication. Not a vulnerability. **False positive.**

3. **Path traversal in file upload** — S3StorageService generates UUID-based storage keys. Original filename is NEVER used in the storage path. Extension is sanitized to alphanumeric only (regex `[a-z0-9]{1,10}`). **False positive.**

4. **Mass assignment via DTOs** — All endpoints use dedicated request DTOs (RegisterRequest, CreateRentalRequest, etc.) with explicit fields. No `@ModelAttribute` usage. Entities are never used as request bodies. MapStruct mappers only map declared fields. Role is hardcoded to `USER` in registration. **False positive.**

5. **XSS** — This is a REST API returning JSON, not rendering HTML. No template engines (Thymeleaf, JSP) are used. React auto-escapes on the frontend. **Not applicable.**

6. **Refresh token uses weak random** — `UUID.randomUUID()` uses `SecureRandom` internally in Java. Cryptographically secure. **False positive.**

7. **SSRF via S3 endpoint** — S3 endpoint is configured via env var at startup, not user-controllable. No endpoint takes URLs as input. **False positive.**

8. **Deserialization attacks** — No `ObjectInputStream`, no `@JsonTypeInfo` with polymorphic typing, no `enableDefaultTyping()`. Jackson default configuration is safe. **False positive.**

9. **Command injection** — No `Runtime.exec()`, `ProcessBuilder`, or shell command execution found anywhere in codebase. **False positive.**

10. **LDAP / NoSQL injection** — No LDAP or NoSQL databases used. **Not applicable.**

11. **Open redirect** — No redirect responses in any controller. All return JSON via ApiResponse. **Not applicable.**
