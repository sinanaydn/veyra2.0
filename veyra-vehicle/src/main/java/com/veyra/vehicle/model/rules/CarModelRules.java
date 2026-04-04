package com.veyra.vehicle.model.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.AlreadyExistsException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.vehicle.model.entity.CarModel;
import com.veyra.vehicle.model.repository.CarModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarModelRules {

    private final CarModelRepository carModelRepository;

    // Aynı marka altında aynı isimde model olamaz
    public void checkIfModelNameExistsForBrand(String name, Long brandId) {
        if (carModelRepository.existsByNameIgnoreCaseAndBrandId(name, brandId))
            throw new AlreadyExistsException(ErrorCodes.CAR_MODEL_ALREADY_EXISTS,
                    "Bu markaya ait model zaten mevcut: " + name);
    }

    // Update: kendi ID'si hariç isim kontrolü
    public void checkIfModelNameExistsForUpdate(String name, Long brandId, Long id) {
        if (carModelRepository.existsByNameIgnoreCaseAndBrandIdAndIdNot(name, brandId, id))
            throw new AlreadyExistsException(ErrorCodes.CAR_MODEL_ALREADY_EXISTS,
                    "Bu markaya ait model zaten mevcut: " + name);
    }

    // Varlık kontrolü + fetch tek seferde — Manager iki kez yazmaz
    public CarModel getByIdOrThrow(Long id) {
        return carModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_MODEL_NOT_FOUND, "Model bulunamadı: " + id));
    }
}
