package com.veyra.payment.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.BusinessRuleException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.payment.entity.Payment;
import com.veyra.payment.enums.PaymentStatus;
import com.veyra.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentRules {

    private final PaymentRepository paymentRepository;

    public Payment getByIdOrThrow(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.PAYMENT_NOT_FOUND, "Ödeme bulunamadı: " + id));
    }

    public void checkIfAlreadyPaid(Long rentalId) {
        if (paymentRepository.existsByRentalIdAndStatus(rentalId, PaymentStatus.COMPLETED))
            throw new BusinessRuleException(
                    ErrorCodes.PAYMENT_ALREADY_DONE, "Bu kiralama için ödeme zaten yapılmış: " + rentalId);
    }
}
