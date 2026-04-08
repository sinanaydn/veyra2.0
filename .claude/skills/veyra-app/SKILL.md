---
name: veyra-app
description: Spring Boot entry point modülü. VeyraApplication main class, SwaggerConfig, application.yml. Tüm modülleri aggregate eder ve fat JAR olarak paketlenir.
---

## Sorumluluk
Spring Boot uygulamasının başlatıldığı modül. Diğer modüllerin tamamına bağımlıdır ve Spring Boot Maven plugin ile fat JAR olarak paketlenir. Çalıştırılabilir tek modül.

## VeyraApplication

```java
@SpringBootApplication(scanBasePackages = "com.veyra")
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.veyra")
@EntityScan(basePackages = "com.veyra")
```

- `scanBasePackages = "com.veyra"` — diğer modüllerdeki `@Component`, `@Service`, `@RestController` taranır
- `@EnableJpaAuditing` — `BaseEntity.createdAt` / `updatedAt` otomatik dolar
- `@EnableJpaRepositories` ve `@EntityScan` — diğer modüllerin repo/entity'lerini bulmak için

## SwaggerConfig
SpringDoc OpenAPI ayarları:
- API title, version, description
- JWT bearer auth scheme tanımı (`Authorization` header)
- Tüm endpoint'lerde JWT token gönderilebilmesi için global security requirement

## application.yml — Önemli Bölümler

### Server
```yaml
server:
  port: 8080
```

### Datasource (PostgreSQL)
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/veyra
    username: veyra
    password: veyra
  jpa:
    hibernate:
      ddl-auto: update         # dev only — prod'da validate
    open-in-view: false        # lazy loading sorunlarını engeller
    show-sql: false
```

### JWT
```yaml
jwt:
  secret: ${JWT_SECRET:veyra-secret-key-must-be-at-least-256-bits-long-for-hs256-algorithm}
  expiration: 86400000   # 24 saat (ms)
```

### Admin Seed
```yaml
veyra:
  admin:
    email: admin@veyra.com
    password: Admin1234!
```
`AdminSeeder` (veyra-auth) okur.

### Springdoc / Actuator
```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

## pom.xml
- Parent: `veyra-api` (multi-module root)
- Tüm modüller dependency olarak listelenir
- `spring-boot-maven-plugin` `repackage` goal'üyle fat JAR üretir

## Docker
- `Dockerfile` — multi-stage build, `eclipse-temurin:25-jre`
- `docker-compose.yml` — sadece PostgreSQL servisi (app lokalde çalışır)
- Production'da app'i de container'a almak için `docker-compose.yml` genişletilebilir

## Bağımlılıklar
- **Tüm modüller** — core, auth, user, vehicle, rental, payment
