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
 * {@code url} alanını çalışma zamanında türetmemiz gerekiyor. MapStruct interface'lerde
 * alan injection desteklemediği için bu pattern standart kabul edilir.
 *
 * {@code url} hiçbir zaman DB'de tutulmaz; her response build'inde
 * storageKey'den taze üretilir. CDN/vendor/format değişimi DB'ye yansımaz.
 */
@Mapper(componentModel = "spring")
public abstract class CarImageMapper {

    @Autowired
    protected StorageService storageService;

    @Mapping(source = "car.id", target = "carId")
    @Mapping(target = "url",
             expression = "java(storageService.getPublicUrl(image.getStorageKey()))")
    public abstract CarImageResponse toResponse(CarImage image);

    public abstract List<CarImageResponse> toResponseList(List<CarImage> images);
}
