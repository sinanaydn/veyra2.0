package com.veyra.vehicle.car.entity;

import com.veyra.core.entity.BaseEntity;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.model.entity.CarModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Entity
@Table(name = "cars", indexes = {
        @Index(name = "idx_car_status_deleted", columnList = "status, deleted"),
        @Index(name = "idx_car_model", columnList = "model_id, deleted")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Car extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private CarModel model;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int doors;

    @Column(nullable = false)
    private int baggages;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CarStatus status = CarStatus.AVAILABLE;
}