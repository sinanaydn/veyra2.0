package com.veyra.vehicle.car.specification;

import com.veyra.vehicle.car.dto.request.CarFilterRequest;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.model.entity.CarModel;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class CarSpecification {

    private CarSpecification() {}

    public static Specification<Car> withFilters(CarFilterRequest filter) {
        return (root, _, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<Car, CarModel> modelJoin = null;

            if (filter.modelId() != null) {
                modelJoin = root.join("model", JoinType.INNER);
                predicates.add(cb.equal(modelJoin.get("id"), filter.modelId()));
            }

            if (filter.brandId() != null) {
                if (modelJoin == null) {
                    modelJoin = root.join("model", JoinType.INNER);
                }
                var brandJoin = modelJoin.join("brand", JoinType.INNER);
                predicates.add(cb.equal(brandJoin.get("id"), filter.brandId()));
            }

            if (filter.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dailyPrice"), filter.minPrice()));
            }
            if (filter.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dailyPrice"), filter.maxPrice()));
            }
            if (filter.minYear() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("year"), filter.minYear()));
            }
            if (filter.maxYear() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("year"), filter.maxYear()));
            }
            if (filter.fuelType() != null) {
                predicates.add(cb.equal(root.get("fuelType"), filter.fuelType()));
            }
            if (filter.transmission() != null) {
                predicates.add(cb.equal(root.get("transmission"), filter.transmission()));
            }
            if (Boolean.TRUE.equals(filter.available())) {
                predicates.add(cb.equal(root.get("status"), CarStatus.AVAILABLE));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
