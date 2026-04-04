package com.veyra.vehicle.brand.repository;

import com.veyra.vehicle.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {

    boolean existsByNameIgnoreCase(String name);

    // Update sırasında kendi kaydı hariç isim kontrolü
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}