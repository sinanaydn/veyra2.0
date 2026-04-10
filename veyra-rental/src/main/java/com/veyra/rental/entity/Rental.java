package com.veyra.rental.entity;

import com.veyra.core.entity.BaseEntity;
import com.veyra.rental.enums.RentalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rentals", indexes = {
        @Index(name = "idx_rental_car_status", columnList = "carId, status, deleted"),
        @Index(name = "idx_rental_user_status", columnList = "userId, status, deleted")
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Rental extends BaseEntity {

    // Soft reference — veyra-vehicle modülüne JPA FK yok (modüler mimari)
    @Column(nullable = false)
    private Long carId;

    // Soft reference — veyra-user modülüne JPA FK yok
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RentalStatus status = RentalStatus.ACTIVE;
}
