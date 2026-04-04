package com.veyra.vehicle.car.repository;

import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {

    List<Car> findAllByStatus(CarStatus status);

    List<Car> findAllByModelId(Long modelId);
}