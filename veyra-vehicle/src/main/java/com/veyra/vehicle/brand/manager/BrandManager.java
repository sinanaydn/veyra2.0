package com.veyra.vehicle.brand.manager;

import com.veyra.vehicle.brand.dto.request.CreateBrandRequest;
import com.veyra.vehicle.brand.dto.request.UpdateBrandRequest;
import com.veyra.vehicle.brand.dto.response.BrandResponse;
import com.veyra.vehicle.brand.mapper.BrandMapper;
import com.veyra.vehicle.brand.repository.BrandRepository;
import com.veyra.vehicle.brand.rules.BrandRules;
import com.veyra.vehicle.brand.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandManager implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandRules      brandRules;
    private final BrandMapper     brandMapper;

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse create(CreateBrandRequest request) {
        brandRules.checkIfBrandNameExists(request.getName());
        var brand = brandRepository.save(brandMapper.toEntity(request));
        return brandMapper.toResponse(brand);
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public BrandResponse update(Long id, UpdateBrandRequest request) {
        var brand = brandRules.getByIdOrThrow(id);
        brandRules.checkIfBrandNameExistsForUpdate(request.getName(), id);
        brand.setName(request.getName());
        return brandMapper.toResponse(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(Long id) {
        return brandMapper.toResponse(brandRules.getByIdOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("brands")
    public List<BrandResponse> getAll() {
        return brandRepository.findAll()
                .stream()
                .map(brandMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    @CacheEvict(value = "brands", allEntries = true)
    public void delete(Long id) {
        var brand = brandRules.getByIdOrThrow(id);
        brand.setDeleted(true);
        brandRepository.save(brand);
    }
}
