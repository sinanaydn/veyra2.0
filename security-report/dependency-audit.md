# Dependency Security Audit

## Project Overview
- **Build Tool:** Maven (multi-module)
- **Java Version:** 25
- **Spring Boot:** 4.0.3
- **Project Version:** 0.0.1-SNAPSHOT

## Complete Dependency Tree

### Parent POM (BOM)
| Dependency | Version | Source |
|---|---|---|
| spring-boot-starter-parent | 4.0.3 | Parent |
| lombok | 1.18.38 | Property |
| mapstruct | 1.6.3 | Property |
| jjwt-api/impl/jackson | 0.12.6 | Property |
| springdoc-openapi | 2.8.6 | Property |
| aws-sdk-bom (s3) | 2.30.0 | Property |

### Common (All Modules)
- spring-boot-starter-web (inherited)
- spring-boot-starter-data-jpa (inherited)
- spring-boot-starter-validation (inherited)
- mapstruct:1.6.3
- lombok:1.18.38 (optional)

### Per-Module Dependencies
| Module | Additional Dependencies |
|---|---|
| veyra-core | spring-security-core, aws-sdk-s3 |
| veyra-auth | veyra-core, veyra-user, spring-boot-starter-security, jjwt-* |
| veyra-user | veyra-core |
| veyra-vehicle | veyra-core, spring-security-core |
| veyra-rental | veyra-core, veyra-vehicle, veyra-user, spring-security-core |
| veyra-payment | veyra-core, veyra-rental, veyra-vehicle, veyra-user, spring-security-core |
| veyra-app | all modules, postgresql (runtime), springdoc, spring-boot-starter-actuator, spring-boot-starter-cache, spring-dotenv:4.0.0 |

## Security Findings

### CRITICAL
1. **SNAPSHOT versions in production** — All modules use `0.0.1-SNAPSHOT`. Non-deterministic builds.

### HIGH
1. **Java 25 (non-LTS)** — Not a Long-Term Support release. Limited security patching window. Requires `--add-opens` hacks for Lombok.
2. **AWS SDK v2.30.0 outdated** — Current stable is 2.40+. May miss S3 security patches.
3. **No OWASP dependency-check plugin** — No automated CVE scanning configured.

### MEDIUM
1. **JJWT 0.12.6** — Used on critical auth path. Verify no CVEs; check for 0.13.x.
2. **PostgreSQL driver not explicitly pinned** — Relies on Spring Boot parent resolution.
3. **No Maven Enforcer plugin** — No guardrails against dependency conflicts.
4. **SpringDoc 2.8.6 outdated** — Swagger UI may have XSS/CSRF in older versions.

### LOW
1. **spring-dotenv 4.0.0** — Third-party maintained, consider native Spring alternatives.
2. **MapStruct 1.6.3** — 1.7.x available.
3. **Lombok 1.18.38** — Requires Java 25 workarounds via --add-opens flags.

## Good Practices Found
- Centralized version management in parent pom
- No circular dependencies
- No shaded/bundled dependencies
- Proper scope annotations (runtime, test)
- Clean module hierarchy (veyra-core at bottom)

## Recommendations
1. Remove `-SNAPSHOT` for production builds
2. Downgrade to Java 21 LTS or verify all deps support Java 25
3. Add maven-enforcer-plugin and OWASP dependency-check-maven
4. Upgrade AWS SDK to 2.40+, SpringDoc to 2.9+, MapStruct to 1.7.x
5. Pin PostgreSQL driver version explicitly
