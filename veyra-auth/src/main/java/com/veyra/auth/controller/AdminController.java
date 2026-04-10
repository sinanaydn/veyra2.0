package com.veyra.auth.controller;

import com.veyra.auth.dto.request.ChangeRoleRequest;
import com.veyra.auth.service.AuthService;
import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstants.ADMIN)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {

        authService.changeRole(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Kullanıcı rolü güncellendi"));
    }
}
