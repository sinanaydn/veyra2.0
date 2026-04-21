package com.veyra.rental.controller;

import com.veyra.core.constants.ApiConstants;
import com.veyra.core.response.ApiResult;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import com.veyra.core.response.PageResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.service.RentalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

/**
 * Rental endpoint'leri. Tümü authenticated — getAll ve complete ADMIN-only.
 * Ortak hata yanıtları {@code OpenApiOperationCustomizer} tarafından eklenir.
 */
@Slf4j
@Tag(name = "Rentals")
@RestController
@RequestMapping(ApiConstants.RENTALS)
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @Operation(
            summary = "Kiralama oluştur",
            description = """
                    Yeni rental kaydı açar (status: `PENDING`).
                    Tarih çakışması varsa 422 `RENTAL_DATE_CONFLICT`.
                    **Yetki:** authenticated (USER veya ADMIN).
                    """,
            responses = @ApiResponse(
                    responseCode = "422", ref = "#/components/responses/UnprocessableEntity")
    )
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<RentalResponse>> create(
            @Valid @RequestBody CreateRentalRequest request,
            Authentication authentication) {
        return ResponseEntity.status(201).body(
                ApiResult.created(rentalService.create(request, authentication.getName())));
    }

    @Operation(
            summary = "Kiralamayı tamamla",
            description = "Rental durumu `COMPLETED` yapılır. **Yetki:** ROLE_ADMIN."
    )
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<RentalResponse>> complete(
            @PathVariable Long id,
            Authentication authentication) {
        RentalResponse response = rentalService.complete(id);
        log.info("Admin {} completed rental {}", authentication.getName(), id);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    @Operation(
            summary = "Kiralamayı iptal et",
            description = """
                    USER sadece kendi rental'ını, ADMIN her rental'ı iptal edebilir.
                    **Yetki:** authenticated.
                    """
    )
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<RentalResponse>> cancel(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResult.success(
                rentalService.cancel(id, authentication.getName(), isAdmin)));
    }

    @Operation(
            summary = "Kiralama detayı",
            description = "USER yalnız kendi rental'larına, ADMIN hepsine erişebilir. **Yetki:** authenticated."
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<RentalResponse>> getById(
            @PathVariable Long id,
            Authentication authentication) {
        boolean isAdmin = SecurityUtils.isAdmin(authentication);
        return ResponseEntity.ok(ApiResult.success(
                rentalService.getById(id, authentication.getName(), isAdmin)));
    }

    @Operation(
            summary = "Kendi kiralamalarım",
            description = """
                    Giriş yapmış kullanıcının rental listesi (sayfalı).
                    Query: `?page=0&size=20&sort=createdAt,desc`
                    **Yetki:** authenticated.
                    """
    )
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<ApiResult<PageResponse<RentalResponse>>> getMyRentals(
            Authentication authentication,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResult.success(
                rentalService.getMyRentals(authentication.getName(), pageable)));
    }

    @Operation(
            summary = "Tüm kiralamalar (admin)",
            description = """
                    Opsiyonel `userId` query param'ı ile kullanıcı filtresi.
                    **Yetki:** ROLE_ADMIN.
                    """
    )
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResult<PageResponse<RentalResponse>>> getAll(
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        var result = userId != null
                ? rentalService.getAllByUserId(userId, pageable)
                : rentalService.getAll(pageable);
        return ResponseEntity.ok(ApiResult.success(result));
    }
}
