package com.veyra.rental.service;

import com.veyra.core.response.PageResponse;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import org.springframework.data.domain.Pageable;

public interface RentalService {

    RentalResponse create(CreateRentalRequest request, String email);

    RentalResponse complete(Long id);

    RentalResponse cancel(Long id, String email, boolean isAdmin);

    RentalResponse getById(Long id, String email, boolean isAdmin);

    PageResponse<RentalResponse> getAll(Pageable pageable);

    PageResponse<RentalResponse> getAllByUserId(Long userId, Pageable pageable);

    PageResponse<RentalResponse> getMyRentals(String email, Pageable pageable);
}
