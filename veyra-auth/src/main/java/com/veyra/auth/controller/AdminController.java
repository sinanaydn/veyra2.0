package com.veyra.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veyra.auth.dto.request.ChangeRoleRequest;
import com.veyra.auth.service.AuthService;
import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Admin-only endpoint'ler.
 * {@code OpenApiOperationCustomizer} class-level {@code @PreAuthorize}'ı görünce
 * tüm method'lara otomatik 401/403 ekler, {@code @PathVariable} görünce 404,
 * {@code @RequestBody} görünce 400 ekler.
 */
@Tag(name = "Admin")
@RestController
@RequestMapping(ApiConstants.ADMIN)
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AuthService authService;

    @Operation(
            summary = "Kullanıcı rolünü değiştir",
            description = "Belirtilen kullanıcıyı ADMIN veya USER rolüne atar. **Yetki:** ROLE_ADMIN."
    )
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResult<Void>> changeRole(
            @PathVariable Long userId,
            @Valid @RequestBody ChangeRoleRequest request) {

        authService.changeRole(userId, request);
        return ResponseEntity.ok(ApiResult.success(null, "Kullanıcı rolü güncellendi"));
    }
}
