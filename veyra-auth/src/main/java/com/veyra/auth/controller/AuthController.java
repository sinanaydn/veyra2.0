package com.veyra.auth.controller;

import com.veyra.auth.dto.request.LoginRequest;
import com.veyra.auth.dto.request.RefreshRequest;
import com.veyra.auth.dto.request.RegisterRequest;
import com.veyra.auth.dto.response.AuthResponse;
import com.veyra.auth.service.AuthService;
import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Auth endpoint'leri. Tüm method'lar {@code @SecurityRequirements} (boş) ile
 * public'e işaretli — global bearer auth override edilir.
 *
 * <p><b>Ortak hata yanıtları</b> (400/429/401 vb.) {@code OpenApiOperationCustomizer}
 * tarafından otomatik eklenir. Aşağıdaki her method yalnızca kendi özel
 * yanıtlarını tanımlar (409 conflict, login 401, vb.).</p>
 */
@Tag(name = "Auth")
@RestController
@RequestMapping(ApiConstants.AUTH)
@SecurityRequirements
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Yeni kullanıcı kaydı",
            description = """
                    Hesap oluşturur, profil bilgisini `veyra-user`'a kaydeder,
                    access + refresh token döner.

                    **Public endpoint** — Rate limit: 5 istek / 60 sn (IP başına).
                    """,
            responses = {
                    @ApiResponse(responseCode = "201", description = "Kayıt başarılı"),
                    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<ApiResult<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity
                .status(201)
                .body(ApiResult.created(authService.register(request)));
    }

    @Operation(
            summary = "Giriş yap",
            description = """
                    E-posta + şifre ile giriş. 5 başarısız denemede hesap kilitlenir
                    (`errorCode: ACCOUNT_LOCKED`).

                    **Public endpoint** — Rate limit: 5 istek / 60 sn.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Giriş başarılı"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResult<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(ApiResult.success(authService.login(request)));
    }

    @Operation(
            summary = "Access token yenile",
            description = "Geçerli bir refresh token ile yeni access token alır.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Yeni token üretildi"),
                    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized")
            }
    )
    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<AuthResponse>> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(ApiResult.success(authService.refresh(request)));
    }

    @Operation(
            summary = "Çıkış yap",
            description = "Refresh token'ı DB'de revoke eder. Access token 15 dk daha geçerli kalır."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(
            @Valid @RequestBody RefreshRequest request) {

        authService.logout(request);
        return ResponseEntity.ok(ApiResult.success(null));
    }
}
