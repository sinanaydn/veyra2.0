package com.veyra.vehicle.image.service;

import com.veyra.vehicle.image.dto.request.ReorderImagesRequest;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * CarImage domain'inin public kontratı.
 * Controller bu interface'e bağımlıdır, implementation'ı ({@code CarImageManager}) bilmez.
 */
public interface CarImageService {

    /**
     * Yeni bir görsel yükler. İlk görsel otomatik olarak kapak (primary) olur.
     * Yeni görsel her zaman listenin sonuna eklenir (maxOrder + 1).
     */
    CarImageResponse upload(Long carId, MultipartFile file);

    /**
     * Bir araca ait tüm görselleri displayOrder'a göre sıralı döner.
     */
    List<CarImageResponse> getByCarId(Long carId);

    /**
     * Görseli siler — hem storage'dan (anında) hem DB'den (soft delete).
     * Silinen görsel kapaksa, kalan görseller arasından displayOrder'ı en küçük olan yeni kapak olur.
     */
    void delete(Long carId, Long imageId);

    /**
     * Bir görseli araç için yeni kapak (primary) olarak işaretler.
     * Eski kapak varsa bayrağı kaldırılır — aynı araç için aynı anda en fazla 1 primary.
     */
    CarImageResponse setPrimary(Long carId, Long imageId);

    /**
     * Bir araca ait görsellerin sıralamasını toplu günceller.
     * Gönderilen tüm imageId'ler o araca ait olmalı, aksi takdirde hata.
     */
    List<CarImageResponse> reorder(Long carId, ReorderImagesRequest request);
}
