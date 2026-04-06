package com.veyra.rental.repository;

import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    List<Rental> findAllByUserId(Long userId);

    List<Rental> findAllByCarId(Long carId);

    boolean existsByCarIdAndStatus(Long carId, RentalStatus status);
}
