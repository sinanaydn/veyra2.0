package com.veyra.payment.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.veyra.core.response.PageResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;
import com.veyra.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoint'leri. Tümü authenticated — getAll ADMIN-only.
 * Ortak hata yanıtları {@code OpenApiOperationCustomizer} tarafından eklenir.
 */
@Tag(name = "Payments")
@RestController
@RequestMapping(ApiConstants.PAYMENTS)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(
            summary = "Ödeme yap",
            description = """
                    Simüle ödeme işler, rental durumunu `CONFIRMED` yapar.
                    `X-Idempotency-Key` header'ı ile aynı ödeme iki kez işlenmez
                    (retry-safe — network timeout'ta client aynı key'le tekrar deneyebilir).
                    **Yetki:** authenticated (USER veya ADMIN).
                    """,
            responses = @ApiResponse(
                    responseCode = "422", ref = "#/components/responses/UnprocessableEntity")
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<PaymentResponse>> pay(
            @Valid @RequestBody CreatePaymentRequest request,
            @Parameter(
                    description = "Idempotency anahtarı (UUID önerilir). Aynı key + aynı body → aynı response.",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.status(201).body(
                ApiResult.created(paymentService.pay(request, authentication.getName(), isAdmin, idempotencyKey)));
    }

    @Operation(
            summary = "Ödeme detayı",
            description = "USER yalnız kendi ödemesine, ADMIN hepsine erişebilir. **Yetki:** authenticated."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<PaymentResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResult.success(
                paymentService.getById(id, authentication.getName(), isAdmin)));
    }

    @Operation(
            summary = "Kendi ödemelerim",
            description = "Giriş yapmış kullanıcının ödeme listesi (sayfalı). **Yetki:** authenticated."
    )
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<PageResponse<PaymentResponse>>> getMyPayments(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.success(
                paymentService.getMyPayments(authentication.getName(), pageable)));
    }

    @Operation(
            summary = "Tüm ödemeler (admin)",
            description = "Opsiyonel `userId` query param'ı ile kullanıcı filtresi. **Yetki:** ROLE_ADMIN."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PageResponse<PaymentResponse>>> getAll(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        var result = userId != null
                ? paymentService.getAllByUserId(userId, pageable)
                : paymentService.getAll(pageable);
        return ResponseEntity.ok(ApiResult.success(result));
    }
}
