package com.veyra.payment.repository;

import com.veyra.payment.entity.Payment;
import com.veyra.payment.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findAllByUserId(Long userId);

    List<Payment> findAllByRentalId(Long rentalId);

    boolean existsByRentalIdAndStatus(Long rentalId, PaymentStatus status);
}
