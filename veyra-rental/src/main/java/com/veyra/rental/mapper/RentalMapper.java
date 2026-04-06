package com.veyra.rental.mapper;

import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.entity.Rental;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RentalMapper {

    RentalResponse toResponse(Rental rental);
}
