package com.veyra.payment.repository;

import com.veyra.payment.entity.Payment;
import com.veyra.payment.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    Page<Payment> findAllByUserId(Long userId, Pageable pageable);

    List<Payment> findAllByRentalId(Long rentalId);

    boolean existsByRentalIdAndStatus(Long rentalId, PaymentStatus status);
}
