package com.veyra.vehicle.car.manager;

import com.veyra.core.constants.ErrorCodes;
import com.veyra.core.exception.ResourceNotFoundException;
import com.veyra.core.response.PageResponse;
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
import org.springframework.data.domain.Pageable;
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
        var car = carRepository.findByIdWithModelAndBrand(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCodes.CAR_NOT_FOUND, "Araç bulunamadı: " + id));
        return carMapper.toResponse(car);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponse> getAll() {
        return carRepository.findAllWithModelAndBrand()
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CarResponse> getAll(Pageable pageable) {
        return new PageResponse<>(carRepository.findAllWithModelAndBrand(pageable)
                .map(carMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarResponse> getAvailable() {
        return carRepository.findAllByStatusWithModelAndBrand(CarStatus.AVAILABLE)
                .stream()
                .map(carMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CarResponse> getAvailable(Pageable pageable) {
        return new PageResponse<>(carRepository.findAllByStatusWithModelAndBrand(CarStatus.AVAILABLE, pageable)
                .map(carMapper::toResponse));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        var car = carRules.getByIdOrThrow(id);
        carRules.checkIfCarCanBeDeleted(car);
        car.setDeleted(true);
        carRepository.save(car);
    }

    @Override
    @Transactional
    public void markAsRented(Long carId) {
        var car = carRules.getByIdOrThrow(carId);
        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);
    }

    @Override
    @Transactional
    public void markAsAvailable(Long carId) {
        var car = carRules.getByIdOrThrow(carId);
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);
    }
}