package com.veyra.app.config;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;

import java.lang.reflect.Parameter;

/**
 * Tüm endpoint'lere ortak hata yanıtlarını **otomatik** ekler.
 *
 * Amaç: controller'larda her endpoint'e 10-15 satırlık mükerrer
 * {@code @ApiResponses} bloğu yazmaktan kurtulmak (DRY).
 *
 * Kurallar:
 *  - Public endpoint (method-level {@code @SecurityRequirements} boş array ile) → 429 (rate limit)
 *  - Değilse → 401 (token missing/invalid)
 *  - Sınıf veya method üzerinde {@code @PreAuthorize} → 403 (forbidden)
 *  - Herhangi bir path variable → 404 (not found)
 *  - İstek gövdesi / multipart → 400 (validation)
 *
 * Controller'lar sadece endpoint-spesifik yanıtları (201, 204, 409, 422,
 * login için 401 vb.) kendileri tanımlar — bu customizer onları silmez,
 * {@code putIfAbsent} mantığıyla çalışır.
 */
@Configuration
public class OpenApiOperationCustomizer {

    @Bean
    public OperationCustomizer commonResponsesCustomizer() {
        return (operation, handlerMethod) -> {
            ApiResponses responses = operation.getResponses();
            if (responses == null) {
                responses = new ApiResponses();
                operation.setResponses(responses);
            }

            boolean isPublic = isMarkedPublic(handlerMethod.getMethod(), handlerMethod.getBeanType());
            boolean isAdminProtected = hasPreAuthorize(handlerMethod);
            boolean hasPathVar = hasAnyParameterAnnotation(handlerMethod.getMethod(), PathVariable.class);
            boolean hasBody = hasAnyParameterAnnotation(handlerMethod.getMethod(), RequestBody.class)
                    || hasAnyParameterAnnotation(handlerMethod.getMethod(), RequestPart.class);

            if (isPublic) {
                putIfAbsent(responses, "429", "TooManyRequests");
            } else {
                putIfAbsent(responses, "401", "Unauthorized");
            }

            if (isAdminProtected) {
                putIfAbsent(responses, "403", "Forbidden");
            }

            if (hasPathVar) {
                putIfAbsent(responses, "404", "NotFound");
            }

            if (hasBody) {
                putIfAbsent(responses, "400", "BadRequest");
            }

            return operation;
        };
    }

    // ---- helpers -----------------------------------------------------------

    private static boolean isMarkedPublic(java.lang.reflect.Method method, Class<?> beanType) {
        SecurityRequirements methodSr = method.getAnnotation(SecurityRequirements.class);
        if (methodSr != null && methodSr.value().length == 0) {
            return true;
        }
        SecurityRequirements classSr = beanType.getAnnotation(SecurityRequirements.class);
        return classSr != null && classSr.value().length == 0;
    }

    private static boolean hasPreAuthorize(org.springframework.web.method.HandlerMethod handler) {
        return handler.getMethod().isAnnotationPresent(PreAuthorize.class)
                || handler.getBeanType().isAnnotationPresent(PreAuthorize.class);
    }

    private static boolean hasAnyParameterAnnotation(java.lang.reflect.Method method,
                                                     Class<? extends java.lang.annotation.Annotation> anno) {
        for (Parameter p : method.getParameters()) {
            if (p.isAnnotationPresent(anno)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ekler yalnız o kod yoksa — controller'ın manuel tanımladığı response'ları ezmez.
     * {@code $ref} kullanıldığında OpenAPI spec'e göre diğer alanlar (description vs.)
     * yok sayılır, bu yüzden sadece refName yeterli.
     */
    private static void putIfAbsent(ApiResponses responses, String code, String refName) {
        if (responses.containsKey(code)) {
            return;
        }
        responses.addApiResponse(code, new ApiResponse().$ref("#/components/responses/" + refName));
    }
}
