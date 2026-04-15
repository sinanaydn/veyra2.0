# Security Assessment Report

**Project:** Veyra RentACar REST API  
**Date:** 2026-04-13  
**Scanner:** security-check v1.0.0 (AI-powered static analysis)  
**Risk Score:** 5.3/10 (Medium Risk)

---

## Executive Summary

A security assessment was performed on the Veyra RentACar API — a Spring Boot 4.0.3 multi-module Java REST API with JWT authentication, PostgreSQL database, and S3-compatible object storage. The scan analyzed approximately 70+ Java source files across 7 Maven modules using 40+ vulnerability detection skills.

The application demonstrates **strong security fundamentals**: stateless JWT auth with token rotation, BCrypt password hashing, comprehensive input validation with magic byte verification for file uploads, parameterized JPA queries preventing SQL injection, proper authorization with ownership checks, and all secrets externalized to environment variables.

However, several **medium-severity gaps** exist in authentication hardening, response headers, rate limiting coverage, and operational security that should be addressed before production deployment.

### Key Metrics
| Metric | Value |
|--------|-------|
| Total Findings | 16 |
| Critical | 0 |
| High | 2 |
| Medium | 7 |
| Low | 5 |
| Info | 2 |

### Top Risks
1. **Weak admin password with no strength validation** — Admin account bypasses the password complexity rules enforced for regular users
2. **No account lockout** — Failed login attempts are not tracked per-account, enabling distributed brute-force attacks
3. **User enumeration via registration** — Registration endpoint confirms whether an email is already registered

---

## Scan Statistics

| Statistic | Value |
|-----------|-------|
| Files Scanned | ~70 Java files + config |
| Languages Detected | Java (100%) |
| Frameworks Detected | Spring Boot 4.0.3, Spring Security, Spring Data JPA, JJWT |
| Skills Executed | 40+ |
| Findings Before Verification | 23 |
| False Positives Eliminated | 11 |
| Final Verified Findings | 16 |

### Finding Distribution

| Vulnerability Category | High | Medium | Low | Info |
|-----------------------|------|--------|-----|------|
| Authentication | 2 | 1 | 1 | - |
| Authorization | - | - | - | - |
| Session Management | - | 1 | 1 | - |
| API Security | - | 2 | - | 1 |
| Data Exposure | - | 1 | - | - |
| Security Headers | - | 1 | - | - |
| Infrastructure | - | - | 2 | - |
| Configuration | - | 1 | 1 | - |
| Race Conditions | - | - | - | 1 |

---

## High Findings

### VULN-001: Weak Admin Password Without Strength Validation

**Severity:** High  
**Confidence:** 90/100  
**CWE:** CWE-521 — Weak Password Requirements  
**OWASP:** A07:2021 — Identification and Authentication Failures

**Location:** `veyra-auth/src/main/java/com/veyra/auth/config/AdminSeeder.java:37-42`

**Description:**  
The AdminSeeder creates the highest-privilege account in the system but only validates that the password is non-blank. Regular users must meet strict requirements (10-128 chars, uppercase, lowercase, digit, special char) via `RegisterRequest` validation. The admin account bypasses ALL of these checks. The actual `.env` file contains `ADMIN_PASSWORD=ss12ss12!` — only 9 characters, which wouldn't even pass the 10-char minimum for regular users.

**Vulnerable Code:**
```java
// AdminSeeder.java:37-42
if (adminPassword == null || adminPassword.isBlank()) {
    throw new IllegalStateException(
            "ADMIN_PASSWORD env var set edilmemiş. " +
            "Uygulama güvensiz varsayılan şifre olmadan başlatılamaz.");
}
// No strength validation! Any non-blank string is accepted.
```

**Impact:**  
An attacker could brute-force the admin account with a relatively small dictionary. Admin has full system access: user management, vehicle management, rental completion, all data access.

**Remediation:**
```java
@PostConstruct
void validateAdminPassword() {
    if (adminPassword == null || adminPassword.isBlank()) {
        throw new IllegalStateException("ADMIN_PASSWORD env var set edilmemiş.");
    }
    if (adminPassword.length() < 12) {
        throw new IllegalStateException("ADMIN_PASSWORD en az 12 karakter olmalıdır.");
    }
    if (!adminPassword.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!.,?_-]).+$")) {
        throw new IllegalStateException("ADMIN_PASSWORD complexity requirements karşılanmıyor.");
    }
}
```

**References:**
- [CWE-521](https://cwe.mitre.org/data/definitions/521.html)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)

---

### VULN-002: No Account Lockout After Failed Login Attempts

**Severity:** High  
**Confidence:** 85/100  
**CWE:** CWE-307 — Improper Restriction of Excessive Authentication Attempts  
**OWASP:** A07:2021 — Identification and Authentication Failures

**Location:** `veyra-auth/src/main/java/com/veyra/auth/manager/AuthManager.java:88-94`

**Description:**  
The login endpoint has no per-account lockout mechanism. The IP-based rate limit (5 req/60s) provides minimal protection but is bypassable with distributed IPs. An attacker with a botnet can attempt unlimited passwords against a known email address. There's no failed attempt counter, no temporary lock, no CAPTCHA escalation, and no exponential backoff.

**Vulnerable Code:**
```java
// AuthManager.java:88-94
AuthUser authUser = authRules.getByEmailOrThrow(request.getEmail());
if (!passwordEncoder.matches(request.getPassword(), authUser.getPasswordHash())) {
    throw new UnauthorizedException(ErrorCodes.INVALID_CREDENTIALS, "E-posta veya şifre hatalı");
    // No failed attempt tracking!
}
```

**Impact:**  
Distributed brute-force attack against any account. Combined with user enumeration (VULN-003), an attacker can first confirm valid emails then target them with password attacks.

**Remediation:**  
Add a `failed_login_attempts` counter and `locked_until` timestamp to AuthUser entity. After 5 consecutive failures, lock the account for 15 minutes. Reset counter on successful login. Log all failed attempts with IP address.

**References:**
- [CWE-307](https://cwe.mitre.org/data/definitions/307.html)

---

## Medium Findings

### VULN-003: User Enumeration via Registration Endpoint

**Severity:** Medium  
**Confidence:** 85/100  
**CWE:** CWE-204 — Observable Response Discrepancy

**Location:** `veyra-auth/src/main/java/com/veyra/auth/rules/AuthRules.java:27-34`

**Description:**  
Registration returns 409 with "Bu e-posta adresi zaten kayıtlı" when an email exists, allowing attackers to enumerate valid accounts. The login error message is properly generic ("E-posta veya şifre hatalı"), but registration leaks account existence.

**Remediation:**  
Always return 200 OK with "Kayıt işlemi başlatıldı. E-posta adresinizi kontrol edin." For existing accounts, send a "someone tried to register with your email" notification instead.

---

### VULN-004: Missing Security Response Headers

**Severity:** Medium  
**Confidence:** 90/100  
**CWE:** CWE-693 — Protection Mechanism Failure

**Location:** `veyra-auth/src/main/java/com/veyra/auth/config/SecurityConfig.java`

**Description:**  
No explicit security headers configured: X-Frame-Options, X-Content-Type-Options, HSTS, CSP, Referrer-Policy. Spring Security may add some defaults in Spring Boot 4.0.3 but explicit configuration is best practice.

**Remediation:**  
Add `.headers()` configuration block in SecurityConfig's `securityFilterChain()`.

---

### VULN-005: Swagger UI Enabled by Default

**Severity:** Medium  
**Confidence:** 75/100  
**CWE:** CWE-200 — Information Exposure

**Location:** `veyra-app/src/main/resources/application.yml:47-52`

**Description:**  
Swagger UI defaults to enabled and is publicly accessible without authentication. In production, this exposes the full API surface including endpoint paths, schemas, and auth mechanisms.

**Remediation:**  
Change default to `false`: `enabled: ${SWAGGER_ENABLED:false}`.

---

### VULN-006: No Rate Limiting on Authenticated Endpoints

**Severity:** Medium  
**Confidence:** 70/100  
**CWE:** CWE-770 — Allocation of Resources Without Limits

**Location:** `veyra-auth/src/main/java/com/veyra/auth/filter/RateLimitFilter.java:49`

**Description:**  
Authenticated endpoints have zero rate limiting (Bucket.NONE). A valid JWT holder can send unlimited requests, enabling resource exhaustion or data scraping.

**Remediation:**  
Add a per-user authenticated bucket (e.g., 120 req/min extracted from JWT userId claim).

---

### VULN-007: Race Condition on Payment Idempotency Key

**Severity:** Medium  
**Confidence:** 65/100  
**CWE:** CWE-362 — Race Condition (TOCTOU)

**Location:** `veyra-payment/src/main/java/com/veyra/payment/manager/PaymentManager.java:35-39`

**Description:**  
Concurrent requests with the same idempotency key can bypass the check and create duplicate payments. READ COMMITTED isolation doesn't prevent this race.

**Remediation:**  
Add a `UNIQUE` constraint on `idempotency_key` column. Catch `DataIntegrityViolationException` and return the existing payment.

---

### VULN-008: JWT Access Tokens Cannot Be Revoked

**Severity:** Medium  
**Confidence:** 60/100  
**CWE:** CWE-613 — Insufficient Session Expiration

**Location:** `veyra-auth/src/main/java/com/veyra/auth/token/JwtServiceImpl.java:69-72`

**Description:**  
JWT access tokens are stateless with 15-minute lifetime. If compromised, or if a user's role is changed, the old token remains valid until expiry. Logout only revokes refresh tokens.

**Remediation:**  
For critical events (role change, password change, compromise), implement a short-lived JWT blacklist in Redis with 15-minute TTL.

---

### VULN-009: Unbounded Pageable Size Parameter

**Severity:** Medium  
**Confidence:** 60/100  
**CWE:** CWE-400 — Uncontrolled Resource Consumption

**Location:** Multiple controllers

**Description:**  
Paginated endpoints don't explicitly limit max page size. A client can request `?size=999999` to fetch large datasets.

**Remediation:**  
Add to application.yml: `spring.data.web.pageable.max-page-size: 100`.

---

## Low Findings

### VULN-010: Docker Services Expose Ports to Host
- **CWE-284** — Docker ports (5432, 9000, 9001) bound to all interfaces. Bind to 127.0.0.1 or use Docker internal network in production.

### VULN-011: Hardcoded Healthcheck Values in Docker Compose
- **CWE-798** — PostgreSQL healthcheck uses literal `veyra_user`/`veyra_db` instead of env var references.

### VULN-012: .env File Contains Weak Dev Credentials
- **CWE-256** — `.env` exists with weak passwords (admin: `ss12ss12!`, DB: `veyra_pass`). Properly gitignored but weak credentials could propagate to production.

### VULN-013: No Security Audit Logging
- **CWE-778** — No logging for login attempts, registrations, role changes, or admin actions. Only rental completion is logged.

### VULN-014: Login Forces Single-Session (Revokes All Previous Tokens)
- **CWE-613** — `login()` calls `revokeAllByAuthUserId()`, logging out all other devices. Could be used as DoS if attacker knows credentials.

---

## Informational

### VULN-015: DDL Auto-Update as Default
- `ddl-auto: ${DDL_AUTO:update}` — Default should be `validate` for production safety. Current default would auto-modify schema.

### VULN-016: Unpaginated Service Methods Exist
- `RentalManager.getAll()` and `PaymentManager.getAll()` return full lists without pagination. Not currently exposed via API but could be called from future code.

---

## Positive Security Observations

The codebase demonstrates several **strong security practices**:

1. **No SQL Injection** — All queries use JPQL with named parameters or Spring Data JPA derived queries. Zero string concatenation in queries.
2. **Strong Input Validation** — Jakarta Bean Validation with detailed annotations on all request DTOs. Password complexity regex enforced.
3. **Magic Byte File Validation** — File uploads verify both Content-Type header AND actual file magic bytes (JPEG/PNG/WebP). This prevents MIME type spoofing attacks.
4. **UUID-Based Storage Keys** — Original filenames are never used in storage paths, completely eliminating path traversal risks.
5. **Proper Authorization** — Defense-in-depth with SecurityConfig matchers + @PreAuthorize annotations + service-layer ownership checks via `SecurityUtils.checkOwnership()`.
6. **Secrets Externalized** — All credentials use environment variables, no hardcoded secrets in source code.
7. **Token Rotation** — Refresh tokens are rotated on every use (old deleted, new created), preventing replay attacks.
8. **BCrypt Strength 12** — Appropriate work factor for password hashing.
9. **JWT Secret Validation** — Application refuses to start if JWT_SECRET is missing or under 32 bytes.
10. **Trusted Proxy Validation** — X-Forwarded-For header only trusted from known proxies, preventing IP spoofing for rate limiting.
11. **Soft Deletes** — All entities use `@SQLRestriction("deleted = false")` maintaining audit trails.
12. **Global Exception Handler** — Generic "Sunucu hatası" for 500 errors, no stack trace leakage.
13. **Rental Race Condition Prevention** — `carRules.getByIdOrThrowForUpdate()` uses `SELECT ... FOR UPDATE` pessimistic locking to prevent double-booking.

---

## Remediation Roadmap

### Phase 1: Immediate (1-3 days)
Address High findings and critical quick-wins.

| # | Finding | Effort | Impact |
|---|---------|--------|--------|
| 1 | VULN-001: Add admin password strength validation | Low | High |
| 2 | VULN-001: Change admin password to strong value | Low | High |
| 3 | VULN-004: Add security response headers | Low | Medium |
| 4 | VULN-005: Change Swagger default to disabled | Low | Medium |
| 5 | VULN-015: Change DDL_AUTO default to validate | Low | Medium |

### Phase 2: Short-Term (1-2 weeks)
Address remaining Medium findings.

| # | Finding | Effort | Impact |
|---|---------|--------|--------|
| 6 | VULN-002: Implement account lockout mechanism | Medium | High |
| 7 | VULN-003: Fix user enumeration in registration | Low | Medium |
| 8 | VULN-007: Add UNIQUE constraint on idempotency_key | Low | Medium |
| 9 | VULN-009: Configure max-page-size | Low | Medium |
| 10 | VULN-006: Add authenticated rate limiting | Medium | Medium |

### Phase 3: Medium-Term (1-2 months)
Address architecture improvements and operational security.

| # | Finding | Effort | Impact |
|---|---------|--------|--------|
| 11 | VULN-008: Implement JWT blacklist for critical events | High | Medium |
| 12 | VULN-013: Add security audit logging | Medium | Medium |
| 13 | VULN-010: Secure Docker port bindings | Low | Low |
| 14 | VULN-011: Fix Docker healthcheck env vars | Low | Low |

### Phase 4: Hardening (Ongoing)

| # | Recommendation | Effort | Impact |
|---|---------------|--------|--------|
| 15 | Add OWASP dependency-check Maven plugin | Low | Medium |
| 16 | Upgrade outdated dependencies (AWS SDK, SpringDoc, MapStruct) | Medium | Low |
| 17 | Add request ID tracking (X-Request-ID) | Low | Low |
| 18 | Consider downgrading to Java 21 LTS | Medium | Low |
| 19 | Implement HSTS preload | Low | Low |
| 20 | Add structured JSON security logging | Medium | Medium |

---

## Methodology

This assessment was performed using security-check, an AI-powered static analysis tool that uses large language model reasoning to detect security vulnerabilities.

### Pipeline Phases
1. **Reconnaissance** — Automated codebase architecture mapping, technology detection, entry point enumeration
2. **Vulnerability Hunting** — 40+ specialized skills scanned for injection, auth, access control, data exposure, API security, infrastructure, and Java-specific vulnerabilities
3. **Verification** — False positive elimination with reachability analysis, sanitization checks, framework protection verification, and confidence scoring (0-100)
4. **Reporting** — CVSS-aligned severity classification and prioritized remediation roadmap

### Limitations
- Static analysis only — no runtime testing or dynamic analysis performed
- AI-based reasoning may miss vulnerabilities requiring deep domain knowledge
- Confidence scores are estimates, not guarantees
- Custom business logic flaws may require manual review
- Dependency CVE analysis is based on version numbers, not actual vulnerability scanning (recommend adding OWASP dependency-check plugin)

---

## Disclaimer

This security assessment was performed using automated AI-powered static analysis. It does not constitute a comprehensive penetration test or security audit. The findings represent potential vulnerabilities identified through code pattern analysis and LLM reasoning. False positives and false negatives are possible.

This report should be used as a starting point for security remediation, not as a definitive statement of the application's security posture. A professional security audit by qualified security engineers is recommended for production applications handling sensitive data.

Generated by security-check — github.com/ersinkoc/security-check
