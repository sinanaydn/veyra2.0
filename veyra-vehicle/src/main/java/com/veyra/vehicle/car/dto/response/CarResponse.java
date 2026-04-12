package com.veyra.vehicle.car.dto.response;

import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * {@code images} ve {@code primaryImageUrl} alanları MapStruct tarafından doldurulmaz —
 * {@link com.veyra.vehicle.car.manager.CarManager} her read akışında image enrichment
 * yaparak batch query ile doldurur. Bu yüzden @Setter zorunludur.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarResponse {

    private Long id;
    private Long modelId;
    private String modelName;
    private Long brandId;
    private String brandName;
    private int year;
    private int doors;
    private int baggages;
    private BigDecimal dailyPrice;
    private CarStatus status;
    private LocalDateTime createdAt;

    /** Tüm görseller displayOrder'a göre sıralı. Görsel yoksa boş liste. */
    private List<CarImageResponse> images;

    /** Kapak görselinin public URL'i. Kapak yoksa null. List view'lar için kısa yol. */
    private String primaryImageUrl;
}