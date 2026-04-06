package com.veyra.rental.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRentalRequest {

    @NotNull(message = "Araç ID boş bırakılamaz")
    private Long carId;

    @NotNull(message = "Kullanıcı ID boş bırakılamaz")
    private Long userId;

    @NotNull(message = "Başlangıç tarihi boş bırakılamaz")
    @FutureOrPresent(message = "Başlangıç tarihi geçmişte olamaz")
    private LocalDate startDate;

    @NotNull(message = "Bitiş tarihi boş bırakılamaz")
    @Future(message = "Bitiş tarihi bugünden sonra olmalıdır")
    private LocalDate endDate;
}
