package com.veyra.vehicle.brand.rules;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.AlreadyExistsException;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.vehicle.brand.entity.Brand;
import com.veyra.vehicle.brand.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrandRules {

    private final BrandRepository brandRepository;

    public void checkIfBrandNameExists(String name) {
        if (brandRepository.existsByNameIgnoreCase(name))
            throw new AlreadyExistsException(ErrorCodes.BRAND_ALREADY_EXISTS, "Marka zaten mevcut: " + name);
    }

    // Update: kendi ID'si hariç isim kontrolü — aynı isimde kayıt varsa false positive olmaz
    public void checkIfBrandNameExistsForUpdate(String name, Long id) {
        if (brandRepository.existsByNameIgnoreCaseAndIdNot(name, id))
            throw new AlreadyExistsException(ErrorCodes.BRAND_ALREADY_EXISTS, "Marka zaten mevcut: " + name);
    }

    // Varlık kontrolü + fetch tek seferde — Manager iki kez yazmaz
    public Brand getByIdOrThrow(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.BRAND_NOT_FOUND, "Marka bulunamadı: " + id));
    }
}
