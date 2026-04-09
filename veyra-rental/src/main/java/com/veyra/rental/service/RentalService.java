package com.veyra.rental.service;

import com.veyra.core.response.PageResponse;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RentalService {

    RentalResponse create(CreateRentalRequest request, String email);

    RentalResponse complete(Long id);

    RentalResponse cancel(Long id, String email, boolean isAdmin);

    RentalResponse getById(Long id, String email, boolean isAdmin);

    List<RentalResponse> getAll();

    PageResponse<RentalResponse> getAll(Pageable pageable);

    List<RentalResponse> getAllByUserId(Long userId);

    PageResponse<RentalResponse> getAllByUserId(Long userId, Pageable pageable);

    List<RentalResponse> getMyRentals(String email);
}
