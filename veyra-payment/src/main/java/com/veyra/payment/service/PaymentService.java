package com.veyra.payment.service;

import com.veyra.core.response.PageResponse;
import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;
import org.springframework.data.domain.Pageable;

public interface PaymentService {

    PaymentResponse pay(CreatePaymentRequest request, String email, boolean isAdmin, String idempotencyKey);

    PaymentResponse getById(Long id, String email, boolean isAdmin);

    PageResponse<PaymentResponse> getAll(Pageable pageable);

    PageResponse<PaymentResponse> getAllByUserId(Long userId, Pageable pageable);

    PageResponse<PaymentResponse> getMyPayments(String email, Pageable pageable);
}
