package com.veyra.vehicle.image.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Bir araca ait görsellerin sıralamasını toplu günceller.
 * Frontend drag-and-drop sonrası tüm yeni sırayı tek istek ile gönderir.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReorderImagesRequest {

    @NotEmpty(message = "items boş olamaz")
    @Valid
    private List<ReorderImageItem> items;
}
