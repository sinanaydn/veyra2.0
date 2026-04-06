package com.veyra.payment.mapper;

import com.veyra.payment.dto.response.PaymentResponse;
import com.veyra.payment.entity.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentMapper {

    PaymentResponse toResponse(Payment payment);
}
