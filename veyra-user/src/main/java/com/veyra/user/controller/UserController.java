package com.veyra.user.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import com.veyra.core.response.PageResponse;
import com.veyra.user.dto.request.UpdateUserRequest;
import com.veyra.user.dto.response.UserResponse;
import com.veyra.user.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * User endpoint'leri.
 *
 * Yetki kuralları (SecurityConfig ile hizalı, defense-in-depth için method-level
 * {@code @PreAuthorize} de eklenmiştir):
 *  - GET/PUT/DELETE {@code /{id}} ve liste → ADMIN
 *  - DELETE {@code /me} → authenticated (kullanıcı kendi hesabını siler)
 *
 * Ortak hata yanıtları {@code OpenApiOperationCustomizer} tarafından eklenir.
 */
@Tag(name = "Users")
@RestController
@RequestMapping(ApiConstants.USERS)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Tüm kullanıcıları listele",
            description = "Sayfalı kullanıcı listesi. **Yetki:** ROLE_ADMIN."
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PageResponse<UserResponse>>> getAll(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.success(userService.getAll(pageable)));
    }

    @Operation(
            summary = "Kullanıcı güncelle",
            description = "Profil bilgilerini günceller. **Yetki:** ROLE_ADMIN."
    )
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UserResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResult.success(userService.update(id, request)));
    }

    @Operation(
            summary = "Kullanıcı detayı",
            description = "Kullanıcıyı ID ile getirir. **Yetki:** ROLE_ADMIN."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<UserResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResult.success(userService.getById(id)));
    }

    @Operation(
            summary = "Kullanıcı sil (admin)",
            description = "Soft delete. **Yetki:** ROLE_ADMIN."
    )
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Kendi hesabımı sil",
            description = """
                    JWT'den email alınır — id spoofing mümkün değil. `UserDeletedEvent` cascade'i tetiklenir:
                    AuthUser soft-delete edilir ve tüm refresh token'lar revoke edilir.
                    **Yetki:** authenticated.
                    """
    )
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteSelf(Authentication authentication) {
        userService.deleteByEmail(authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
