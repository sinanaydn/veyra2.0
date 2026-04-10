package com.veyra.payment.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import com.veyra.core.response.PageResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;
import com.veyra.payment.service.PaymentService;
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

@RestController
@RequestMapping(ApiConstants.PAYMENTS)
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.status(201).body(
                ApiResponse.created(paymentService.pay(request, authentication.getName(), isAdmin, idempotencyKey)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getById(id, authentication.getName(), isAdmin)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getMyPayments(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                paymentService.getMyPayments(authentication.getName(), pageable)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getAll(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        var result = userId != null
                ? paymentService.getAllByUserId(userId, pageable)
                : paymentService.getAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
