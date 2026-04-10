package com.veyra.vehicle.model.manager;

import com.veyra.vehicle.brand.rules.BrandRules;
import com.veyra.vehicle.model.dto.request.CreateCarModelRequest;
import com.veyra.vehicle.model.dto.request.UpdateCarModelRequest;
import com.veyra.vehicle.model.dto.response.CarModelResponse;
import com.veyra.vehicle.model.entity.CarModel;
import com.veyra.vehicle.model.mapper.CarModelMapper;
import com.veyra.vehicle.model.repository.CarModelRepository;
import com.veyra.vehicle.model.rules.CarModelRules;
import com.veyra.vehicle.model.service.CarModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarModelManager implements CarModelService {

    private final CarModelRepository carModelRepository;
    private final CarModelRules      carModelRules;
    private final BrandRules         brandRules;
    private final CarModelMapper     carModelMapper;

    @Override
    @Transactional
    @CacheEvict(value = "models", allEntries = true)
    public CarModelResponse create(CreateCarModelRequest request) {
        var brand = brandRules.getByIdOrThrow(request.getBrandId());
        carModelRules.checkIfModelNameExistsForBrand(request.getName(), request.getBrandId());

        var carModel = CarModel.builder()
                .name(request.getName())
                .brand(brand)
                .build();

        return carModelMapper.toResponse(carModelRepository.save(carModel));
    }

    @Override
    @Transactional
    @CacheEvict(value = "models", allEntries = true)
    public CarModelResponse update(Long id, UpdateCarModelRequest request) {
        var carModel = carModelRules.getByIdOrThrow(id);
        var brand = brandRules.getByIdOrThrow(request.getBrandId());
        carModelRules.checkIfModelNameExistsForUpdate(request.getName(), request.getBrandId(), id);

        carModel.setName(request.getName());
        carModel.setBrand(brand);

        return carModelMapper.toResponse(carModelRepository.save(carModel));
    }

    @Override
    @Transactional(readOnly = true)
    public CarModelResponse getById(Long id) {
        return carModelMapper.toResponse(carModelRules.getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("models")
    public List<CarModelResponse> getAll() {
        return carModelRepository.findAll()
                .stream()
                .map(carModelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CarModelResponse> getAllByBrandId(Long brandId) {
        brandRules.getByIdOrThrow(brandId); // marka var mı kontrol et
        return carModelRepository.findAllByBrandId(brandId)
                .stream()
                .map(carModelMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "models", allEntries = true)
    public void delete(Long id) {
        var carModel = carModelRules.getByIdOrThrow(id);
        carModel.setDeleted(true);
        carModelRepository.save(carModel);
    }
}
