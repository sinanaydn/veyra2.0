package com.veyra.payment.service;

import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse pay(CreatePaymentRequest request, String email, boolean isAdmin, String idempotencyKey);

    PaymentResponse getById(Long id, String email, boolean isAdmin);

    List<PaymentResponse> getAll();

    List<PaymentResponse> getAllByUserId(Long userId);

    List<PaymentResponse> getMyPayments(String email);
}
