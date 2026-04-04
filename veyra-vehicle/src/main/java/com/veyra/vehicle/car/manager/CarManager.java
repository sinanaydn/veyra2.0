package com.veyra.vehicle.car.manager;

import com.veyra.vehicle.car.dto.request.CreateCarRequest;
import com.veyra.vehicle.car.dto.request.UpdateCarRequest;
import com.veyra.vehicle.car.dto.response.CarResponse;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.car.mapper.CarMapper;
import com.veyra.vehicle.car.repository.CarRepository;
import com.veyra.vehicle.car.rules.CarRules;
import com.veyra.vehicle.car.service.CarService;
import com.veyra.vehicle.model.rules.CarModelRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarManager implements CarService {

    private final CarRepository  carRepository;
    private final CarRules       carRules;
    private final CarModelRules  carModelRules;
    private final CarMapper      carMapper;

    @Override
    @Transactional
    public CarResponse create(CreateCarRequest request) {
        var model = carModelRules.getByIdOrThrow(request.getModelId());

        var car = Car.builder()
                .model(model)
                .year(request.getYear())
                .doors(request.getDoors())
                .baggages(request.getBaggages())
                .dailyPrice(request.getDailyPrice())
                .build();

        return carMapper.toResponse(carRepository.save(car));
    }

    @Override
    @Transactional
    public CarResponse update(Long id, UpdateCarRequest request) {
        var car   = carRules.getByIdOrThrow(id);
        var model = carModelRules.getByIdOrThrow(request.getModelId());

        car.setModel(model);
        car.setYear(request.getYear());
        car.setDoors(request.getDoors());
        car.setBaggages(request.getBaggages());
        car.setDailyPrice(request.getDailyPrice());
        car.setStatus(request.getStatus());

        return carMapper.toResponse(carRepository.save(car));
    }

    @Override
    @Transactional(readOnly = true)
    public CarResponse getById(Long id) {
        return carMapper.toResponse(carRules.getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponse> getAll() {
        return carRepository.findAll()
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponse> getAvailable() {
        return carRepository.findAllByStatus(CarStatus.AVAILABLE)
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var car = carRules.getByIdOrThrow(id);
        car.setDeleted(true);
        carRepository.save(car);
    }
}