package com.veyra.rental.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.service.RentalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiConstants.RENTALS)
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<RentalResponse>> create(
            @Valid @RequestBody CreateRentalRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(
                ApiResponse.created(rentalService.create(request, authentication.getName())));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RentalResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(rentalService.complete(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<RentalResponse>> cancel(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                rentalService.cancel(id, authentication.getName(), isAdmin)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<RentalResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                rentalService.getById(id, authentication.getName(), isAdmin)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResponse<List<RentalResponse>>> getMyRentals(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(rentalService.getMyRentals(authentication.getName())));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<RentalResponse>>> getAll(
            @RequestParam(required = false) Long userId) {
        var result = userId != null
                ? rentalService.getAllByUserId(userId)
                : rentalService.getAll();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

}
