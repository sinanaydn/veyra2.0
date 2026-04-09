package com.veyra.vehicle.car.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.BusinessRuleException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.car.repository.CarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarRules {

    private final CarRepository carRepository;

    // Varlık kontrolü + fetch tek seferde
    public Car getByIdOrThrow(Long id) {
        return carRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_NOT_FOUND, "Araç bulunamadı: " + id));
    }

    // Kiralama oluşturma sırasında kullanılır — satırı kilitler (SELECT ... FOR UPDATE)
    public Car getByIdOrThrowForUpdate(Long id) {
        return carRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_NOT_FOUND, "Araç bulunamadı: " + id));
    }

    // veyra-rental tarafından da kullanılacak kritik kural
    public void checkIfCarAvailable(Car car) {
        if (car.getStatus() != CarStatus.AVAILABLE)
            throw new BusinessRuleException(
                    ErrorCodes.CAR_NOT_AVAILABLE, "Araç şu an uygun değil: " + car.getId());
    }

    public void checkIfCarCanBeDeleted(Car car) {
        if (car.getStatus() == CarStatus.RENTED)
            throw new BusinessRuleException(
                    ErrorCodes.CAR_NOT_AVAILABLE, "Kirada olan araç silinemez: " + car.getId());
    }
}