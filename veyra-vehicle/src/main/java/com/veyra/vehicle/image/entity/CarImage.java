package com.veyra.vehicle.image.entity;

import com.veyra.core.entity.BaseEntity;
import com.veyra.vehicle.car.entity.Car;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Bir {@link Car} varlığına ait tek bir görsel kaydı.
 *
 * Tasarım kararları:
 *  - {@code storageKey} kalıcı kimliktir — public URL burada tutulmaz,
 *    response üretilirken StorageService ile türetilir. Vendor/CDN değişimine dayanıklı.
 *  - {@code storageKey} unique — storage tarafındaki bir obje birden fazla kayda bağlanamaz.
 *  - {@code displayOrder} kullanıcıya gösterim sırası.
 *  - {@code isPrimary} kapak görseli — her araç için en fazla bir tane olmalı (DB değil, service katmanında enforce).
 *  - Soft delete — {@code @SQLRestriction} ile tüm query'ler otomatik filtreler.
 */
@Entity
@Table(name = "car_images", indexes = {
        @Index(name = "idx_carimage_car", columnList = "car_id, deleted"),
        @Index(name = "idx_carimage_car_primary", columnList = "car_id, is_primary, deleted"),
        @Index(name = "idx_carimage_storage_key", columnList = "storage_key", unique = true)
})
@SQLRestriction("deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean isPrimary = false;
}
