package com.veyra.payment.manager;

import com.veyra.core.response.PageResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.payment.dto.request.CreatePaymentRequest;
import com.veyra.payment.dto.response.PaymentResponse;
import com.veyra.payment.entity.Payment;
import com.veyra.payment.mapper.PaymentMapper;
import com.veyra.payment.repository.PaymentRepository;
import com.veyra.payment.rules.PaymentRules;
import com.veyra.payment.service.PaymentService;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.rules.RentalRules;
import com.veyra.user.rules.UserRules;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentManager implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentRules      paymentRules;
    private final RentalRules       rentalRules;
    private final UserRules         userRules;
    private final PaymentMapper     paymentMapper;

    @Override
    @Transactional
    public PaymentResponse pay(CreatePaymentRequest request, String email, boolean isAdmin, String idempotencyKey) {
        if (idempotencyKey != null) {
            var existing = paymentRepository.findByIdempotencyKey(idempotencyKey);
            if (existing.isPresent()) {
                return paymentMapper.toResponse(existing.get());
            }
        }

        Rental rental = rentalRules.getByIdOrThrow(request.getRentalId());
        SecurityUtils.checkOwnership(rental.getUserId(), email, isAdmin, userRules::getUserIdByEmail);

        rentalRules.checkIfRentalIsActive(rental);
        paymentRules.checkIfAlreadyPaid(request.getRentalId());

        var payment = Payment.builder()
                .rentalId(rental.getId())
                .userId(rental.getUserId())
                .amount(rental.getTotalPrice())
                .idempotencyKey(idempotencyKey)
                .build();

        paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id, String email, boolean isAdmin) {
        var payment = paymentRules.getByIdOrThrow(id);
        SecurityUtils.checkOwnership(payment.getUserId(), email, isAdmin, userRules::getUserIdByEmail);
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAll() {
        return paymentRepository.findAll()
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAll(Pageable pageable) {
        return new PageResponse<>(paymentRepository.findAll(pageable).map(paymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getAllByUserId(Long userId) {
        userRules.checkIfUserExists(userId);
        return paymentRepository.findAllByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> getAllByUserId(Long userId, Pageable pageable) {
        userRules.checkIfUserExists(userId);
        return new PageResponse<>(paymentRepository.findAllByUserId(userId, pageable)
                .map(paymentMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(String email) {
        Long userId = userRules.getUserIdByEmail(email);
        return paymentRepository.findAllByUserId(userId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }
}
