package com.veyra.rental.service;

import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;

import java.util.List;

public interface RentalService {

    RentalResponse create(CreateRentalRequest request);

    RentalResponse complete(Long id);   // Araç iade

    RentalResponse cancel(Long id);     // İptal

    RentalResponse getById(Long id);

    List<RentalResponse> getAll();

    List<RentalResponse> getAllByUserId(Long userId);

    List<RentalResponse> getMyRentals(String email);
}
