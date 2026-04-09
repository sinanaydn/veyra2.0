package com.veyra.payment.manager;

import com.veyra.core.exception.ForbiddenException;
import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;
import com.veyra.payment.entity.Payment;
import com.veyra.payment.enums.PaymentStatus;
import com.veyra.payment.mapper.PaymentMapper;
import com.veyra.payment.repository.PaymentRepository;
import com.veyra.payment.rules.PaymentRules;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import com.veyra.rental.rules.RentalRules;
import com.veyra.user.rules.UserRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentManagerOwnershipTest {

    @Mock PaymentRepository paymentRepository;
    @Mock PaymentRules      paymentRules;
    @Mock RentalRules       rentalRules;
    @Mock UserRules         userRules;
    @Mock PaymentMapper     paymentMapper;

    @InjectMocks
    PaymentManager paymentManager;

    // ------------------------------------------------------------------ pay
    @Test
    void pay_shouldThrowForbidden_whenUserPaysForSomeoneElsesRental() {
        Long rentalId   = 5L;
        Long ownerId    = 1L;
        Long attackerId = 99L;

        Rental rental = Rental.builder()
                .userId(ownerId)
                .carId(2L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        CreatePaymentRequest request = new CreatePaymentRequest(rentalId);

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(userRules.getUserIdByEmail("attacker@veyra.com")).thenReturn(attackerId);

        assertThatThrownBy(() ->
                paymentManager.pay(request, "attacker@veyra.com", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("yetkiniz yok");

        // Ödeme kaydedilmemeli
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void pay_shouldSucceed_whenUserPaysForOwnRental() {
        Long rentalId = 5L;
        Long userId   = 1L;

        Rental rental = Rental.builder()
                .userId(userId)
                .carId(2L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        CreatePaymentRequest request = new CreatePaymentRequest(rentalId);

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(userRules.getUserIdByEmail("owner@veyra.com")).thenReturn(userId);
        when(paymentMapper.toResponse(any())).thenReturn(PaymentResponse.builder().build());

        paymentManager.pay(request, "owner@veyra.com", false);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void pay_shouldSucceed_whenAdminPaysForAnyRental() {
        Long rentalId = 5L;
        Long ownerId  = 1L;

        Rental rental = Rental.builder()
                .userId(ownerId)
                .carId(2L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        CreatePaymentRequest request = new CreatePaymentRequest(rentalId);

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(paymentMapper.toResponse(any())).thenReturn(PaymentResponse.builder().build());

        // isAdmin=true — ownership kontrolü atlanmalı, ödeme kaydedilmeli, ekstra DB sorgusu olmamalı
        paymentManager.pay(request, "admin@veyra.com", true);

        verify(paymentRepository).save(any(Payment.class));
        verify(userRules, never()).getUserIdByEmail(any());
    }

    // ------------------------------------------------------------------ getById
    @Test
    void getById_shouldThrowForbidden_whenUserViewsSomeoneElsesPayment() {
        Long paymentId  = 10L;
        Long ownerId    = 1L;
        Long attackerId = 77L;

        Payment payment = Payment.builder()
                .userId(ownerId)
                .rentalId(5L)
                .amount(BigDecimal.valueOf(300))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRules.getByIdOrThrow(paymentId)).thenReturn(payment);
        when(userRules.getUserIdByEmail("attacker@veyra.com")).thenReturn(attackerId);

        assertThatThrownBy(() ->
                paymentManager.getById(paymentId, "attacker@veyra.com", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("size ait değil");
    }

    @Test
    void getById_shouldSucceed_whenUserViewsOwnPayment() {
        Long paymentId = 10L;
        Long userId    = 1L;

        Payment payment = Payment.builder()
                .userId(userId)
                .rentalId(5L)
                .amount(BigDecimal.valueOf(300))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRules.getByIdOrThrow(paymentId)).thenReturn(payment);
        when(userRules.getUserIdByEmail("owner@veyra.com")).thenReturn(userId);
        when(paymentMapper.toResponse(payment)).thenReturn(PaymentResponse.builder().build());

        paymentManager.getById(paymentId, "owner@veyra.com", false);

        verify(paymentMapper).toResponse(payment);
    }

    @Test
    void getById_shouldSucceed_whenAdminViewsAnyPayment() {
        Long paymentId = 10L;

        Payment payment = Payment.builder()
                .userId(1L)
                .rentalId(5L)
                .amount(BigDecimal.valueOf(300))
                .status(PaymentStatus.COMPLETED)
                .build();

        when(paymentRules.getByIdOrThrow(paymentId)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(PaymentResponse.builder().build());

        // isAdmin=true — ownership kontrolü atlanmalı
        paymentManager.getById(paymentId, "admin@veyra.com", true);

        verify(userRules, never()).getUserIdByEmail(any());
        verify(paymentMapper).toResponse(payment);
    }
}
