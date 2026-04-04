package com.veyra.vehicle.model.repository;

import com.veyra.vehicle.model.entity.CarModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarModelRepository extends JpaRepository<CarModel, Long> {

    boolean existsByNameIgnoreCaseAndBrandId(String name, Long brandId);

    // Update sırasında kendi kaydı hariç isim kontrolü
    boolean existsByNameIgnoreCaseAndBrandIdAndIdNot(String name, Long brandId, Long id);

    List<CarModel> findAllByBrandId(Long brandId);
}
