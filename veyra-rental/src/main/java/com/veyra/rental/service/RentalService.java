package com.veyra.rental.service;

import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;

import java.util.List;

public interface RentalService {

    RentalResponse create(CreateRentalRequest request, String email);

    RentalResponse complete(Long id);

    RentalResponse cancel(Long id, String email, boolean isAdmin);

    RentalResponse getById(Long id, String email, boolean isAdmin);

    List<RentalResponse> getAll();

    List<RentalResponse> getAllByUserId(Long userId);

    List<RentalResponse> getMyRentals(String email);
}
