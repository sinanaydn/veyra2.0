package com.veyra.vehicle.image.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tek bir görsel için yeni sıra değeri.
 * ReorderImagesRequest içinde liste olarak gönderilir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderImageItem {

    @NotNull(message = "imageId zorunlu")
    private Long imageId;

    @Min(value = 1, message = "displayOrder en az 1 olmalı")
    private int displayOrder;
}
