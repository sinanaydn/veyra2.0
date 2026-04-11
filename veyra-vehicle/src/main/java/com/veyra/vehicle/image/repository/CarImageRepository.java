package com.veyra.vehicle.image.repository;

import com.veyra.vehicle.image.entity.CarImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * CarImage için repository. Tüm metotlar {@code @SQLRestriction("deleted = false")}
 * sayesinde otomatik olarak silinmiş kayıtları dışarıda bırakır.
 */
public interface CarImageRepository extends JpaRepository<CarImage, Long> {

    /**
     * Bir araca ait tüm görselleri displayOrder'a göre sıralı döner.
     * Liste endpoint'i için birincil metod.
     */
    List<CarImage> findByCarIdOrderByDisplayOrderAsc(Long carId);

    /**
     * Limit kontrolü için — araçtaki görsel sayısı (max 10 kuralı için).
     */
    long countByCarId(Long carId);

    /**
     * Bir araçtaki kapak görselini getirir (en fazla 1 tane olmalı).
     * Yeni kapak atanırken eskisinin kapak bayrağını kaldırmak için kullanılır.
     */
    Optional<CarImage> findFirstByCarIdAndIsPrimaryTrue(Long carId);

    /**
     * Bir araçta kaç görsel varsa en yüksek displayOrder'ı döner.
     * Yeni upload'ın sırası {@code max + 1} olarak atanır — gap oluşmaz.
     * COALESCE ile boş sonuç 0 olarak döner (ilk görselde order = 1 olur).
     */
    @Query("SELECT COALESCE(MAX(ci.displayOrder), 0) FROM CarImage ci WHERE ci.car.id = :carId")
    int findMaxDisplayOrderByCarId(@Param("carId") Long carId);

    /**
     * Ownership check için — görsel hem {@code id}'ye sahip olmalı hem de belirtilen araca ait olmalı.
     * Başka bir aracın görselini silmeye çalışan istekleri erken durdurur.
     */
    Optional<CarImage> findByIdAndCarId(Long id, Long carId);
}
