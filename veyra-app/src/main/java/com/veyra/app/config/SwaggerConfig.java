package com.veyra.app.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger UI + OpenAPI şeması.
 * - JWT Bearer desteği (Authorize butonu)
 * - Ortak hata yanıtı bileşeni ({@code ApiErrorResponse}) — her controller @ApiResponse ile referanslar
 * - Tag sıralaması: ön uç menüsünde modül bazlı grup görünümü
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";
    public static final String ERROR_SCHEMA_NAME = "ApiErrorResponse";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Veyra RentACar API")
                        .version("1.0.0")
                        .description("""
                                Araç kiralama sistemi REST API.

                                **Auth akışı:**
                                - `POST /api/v1/auth/login` → `token` + `refreshToken`
                                - Her isteğe `Authorization: Bearer <token>` header'ı
                                - Access token 15 dk, refresh token 7 gün
                                - `POST /api/v1/auth/refresh` ile yenile

                                **Response zarfı:** Tüm endpoint'ler `ApiResult<T>` yapısında döner.
                                Hata durumunda `errorCode` alanı frontend için makine okunabilir kod içerir
                                (örn. `INVALID_CREDENTIALS`, `USER_NOT_FOUND`, `RATE_LIMIT_EXCEEDED`).

                                **Rate limit:**
                                - `/api/v1/auth/**` — 5 istek / 60 sn (IP başına)
                                - Public `GET /cars|/brands|/models/**` — 60 istek / 60 sn (IP başına)
                                - Authenticated endpoint'ler — limit yok
                                - Aşıldığında `429` + `errorCode: RATE_LIMIT_EXCEEDED`

                                **Public endpoint'ler** (token gerektirmez):
                                `GET /brands/**`, `GET /models/**`, `GET /cars/**`, `/auth/login`, `/auth/register`, `/auth/refresh`
                                """)
                        .contact(new Contact()
                                .name("Veyra Backend")
                                .email("backend@veyra.com"))
                        .license(new License().name("Proprietary")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .tags(List.of(
                        new Tag().name("Auth").description("Kayıt, giriş, token yenileme, çıkış"),
                        new Tag().name("Users").description("Kullanıcı profil CRUD"),
                        new Tag().name("Admin").description("Yönetici işlemleri (ROLE_ADMIN)"),
                        new Tag().name("Brands").description("Marka katalog (GET public, yazma ADMIN)"),
                        new Tag().name("Car Models").description("Model katalog (GET public, yazma ADMIN)"),
                        new Tag().name("Cars").description("Araç katalog + filtreleme (GET public, yazma ADMIN)"),
                        new Tag().name("Car Images").description("Araç görselleri (GET public, upload/reorder/delete ADMIN)"),
                        new Tag().name("Rentals").description("Kiralama akışı (authenticated)"),
                        new Tag().name("Payments").description("Ödeme simülasyonu (authenticated, X-Idempotency-Key destekli)")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"))
                        .addSchemas(ERROR_SCHEMA_NAME, apiErrorSchema())
                        .addResponses("BadRequest", errorResponse("400 — Geçersiz istek / doğrulama hatası", "VALIDATION_ERROR"))
                        .addResponses("Unauthorized", errorResponse("401 — Token eksik / geçersiz / süresi dolmuş", "TOKEN_INVALID"))
                        .addResponses("Forbidden", errorResponse("403 — Yetkisiz erişim (rol yetersiz)", "ACCESS_DENIED"))
                        .addResponses("NotFound", errorResponse("404 — Kaynak bulunamadı", "USER_NOT_FOUND"))
                        .addResponses("Conflict", errorResponse("409 — Çakışma (örn. e-posta zaten kayıtlı)", "EMAIL_ALREADY_EXISTS"))
                        .addResponses("UnprocessableEntity", errorResponse("422 — İş kuralı ihlali", "RENTAL_DATE_INVALID"))
                        .addResponses("TooManyRequests", errorResponse("429 — Rate limit aşıldı", "RATE_LIMIT_EXCEEDED")));
    }

    private static Schema<?> apiErrorSchema() {
        return new Schema<>()
                .type("object")
                .description("Ortak hata yanıtı zarfı (tüm endpoint'ler için)")
                .addProperty("success", new Schema<Boolean>().type("boolean").example(false))
                .addProperty("status", new Schema<Integer>().type("integer").example(400))
                .addProperty("message", new StringSchema().example("Doğrulama başarısız"))
                .addProperty("errorCode", new StringSchema()
                        .description("Makine okunabilir hata kodu — frontend bu değere göre UI mesajı gösterir")
                        .example("VALIDATION_ERROR"))
                .addProperty("data", new Schema<>().nullable(true).description("Hata detayı (örn. field errors map'i) veya null"))
                .addProperty("timestamp", new StringSchema().format("date-time"));
    }

    private static ApiResponse errorResponse(String description, String errorCodeExample) {
        Schema<?> ref = new Schema<>().$ref("#/components/schemas/" + ERROR_SCHEMA_NAME);
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(ref).example(exampleFor(errorCodeExample))));
    }

    private static String exampleFor(String errorCode) {
        return """
                {
                  "success": false,
                  "status": 400,
                  "message": "Hata mesajı",
                  "errorCode": "%s",
                  "data": null,
                  "timestamp": "2026-04-20T10:30:00"
                }""".formatted(errorCode);
    }
}
