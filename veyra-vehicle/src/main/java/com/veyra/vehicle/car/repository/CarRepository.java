package com.veyra.vehicle.car.repository;

import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findAllByStatus(CarStatus status);

    Page<Car> findAllByStatus(CarStatus status, Pageable pageable);

    List<Car> findAllByModelId(Long modelId);

    /**
     * Araç satırını SELECT ... FOR UPDATE ile kilitler.
     * Eş zamanlı kiralama oluşturma isteklerinde race condition'ı engeller:
     * ilk işlem kilidi alır, ikincisi commit'e kadar bekler.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Car c WHERE c.id = :id")
    Optional<Car> findByIdForUpdate(@Param("id") Long id);
}