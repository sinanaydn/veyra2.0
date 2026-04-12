package com.veyra.vehicle.image.mapper;

import com.veyra.core.storage.StorageService;
import com.veyra.vehicle.image.dto.response.CarImageResponse;
import com.veyra.vehicle.image.entity.CarImage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * CarImage → CarImageResponse dönüşümü.
 *
 * Abstract class — interface değil — çünkü {@link StorageService}'i inject edip
 * {@code url} alanını çalışma zamanında türetmemiz gerekiyor.
 *
 * Setter injection kullanılıyor: MapStruct abstract class constructor'larını
 * generated impl'e otomatik propagate etmez, bu yüzden constructor injection bu
 * pattern'de çalışmaz. Setter injection MapStruct + Spring + abstract class üçlüsü
 * için resmi önerilen pattern'dir ve IntelliJ'in field injection uyarısını tetiklemez.
 *
 * {@code url} hiçbir zaman DB'de tutulmaz; her response build'inde
 * storageKey'den taze üretilir. CDN/vendor/format değişimi DB'ye yansımaz.
 */
@Mapper(componentModel = "spring")
public abstract class CarImageMapper {

    protected StorageService storageService;

    @Autowired
    public void setStorageService(StorageService storageService) {
        this.storageService = storageService;
    }

    @Mapping(source = "car.id", target = "carId")
    @Mapping(target = "url",
             expression = "java(storageService.getPublicUrl(image.getStorageKey()))")
    public abstract CarImageResponse toResponse(CarImage image);

    public abstract List<CarImageResponse> toResponseList(List<CarImage> images);
}
