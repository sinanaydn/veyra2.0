package com.veyra.rental.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.BusinessRuleException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import com.veyra.rental.repository.RentalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class RentalRules {

    private final RentalRepository rentalRepository;

    public Rental getByIdOrThrow(Long id) {
        return rentalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.RENTAL_NOT_FOUND, "Kiralama bulunamadı: " + id));
    }

    public void checkIfCarAlreadyRented(Long carId) {
        if (rentalRepository.existsByCarIdAndStatus(carId, RentalStatus.ACTIVE))
            throw new BusinessRuleException(
                    ErrorCodes.RENTAL_ALREADY_ACTIVE, "Araç zaten aktif kiralamada: " + carId);
    }

    public void checkIfRentalIsActive(Rental rental) {
        if (rental.getStatus() != RentalStatus.ACTIVE)
            throw new BusinessRuleException(
                    ErrorCodes.RENTAL_NOT_ACTIVE, "Bu kiralama aktif değil: " + rental.getId());
    }

    public void checkIfDatesValid(LocalDate startDate, LocalDate endDate) {
        if (!endDate.isAfter(startDate))
            throw new BusinessRuleException(
                    ErrorCodes.RENTAL_DATE_INVALID, "Bitiş tarihi başlangıç tarihinden sonra olmalıdır");
    }
}
