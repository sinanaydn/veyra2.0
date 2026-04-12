package com.veyra.vehicle.image.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Bir CarImage'ın dışarıya açılan hali.
 *
 * {@code url} alanı DB'de tutulmaz — mapper, StorageService üzerinden her çağrıda
 * {@code storageKey}'den türetir. Böylece CDN/vendor değişimi DB migration gerektirmez.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarImageResponse {

    private Long id;
    private Long carId;
    private String storageKey;
    private String url;
    private String contentType;
    private long sizeBytes;
    private int displayOrder;
    private boolean primary;
}
