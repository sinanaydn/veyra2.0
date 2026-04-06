package com.veyra.payment.service;

import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    PaymentResponse pay(CreatePaymentRequest request);

    PaymentResponse getById(Long id);

    List<PaymentResponse> getAll();

    List<PaymentResponse> getAllByUserId(Long userId);

    List<PaymentResponse> getMyPayments(String email);
}
