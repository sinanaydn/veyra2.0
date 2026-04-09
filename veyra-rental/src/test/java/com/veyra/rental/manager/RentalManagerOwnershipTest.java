package com.veyra.rental.manager;

import com.veyra.core.exception.ForbiddenException;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import com.veyra.rental.mapper.RentalMapper;
import com.veyra.rental.repository.RentalRepository;
import com.veyra.rental.rules.RentalRules;
import com.veyra.user.rules.UserRules;
import com.veyra.vehicle.car.entity.Car;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.car.repository.CarRepository;
import com.veyra.vehicle.car.rules.CarRules;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalManagerOwnershipTest {

    @Mock RentalRepository rentalRepository;
    @Mock RentalRules      rentalRules;
    @Mock CarRules         carRules;
    @Mock CarRepository    carRepository;
    @Mock UserRules        userRules;
    @Mock RentalMapper     rentalMapper;

    @InjectMocks
    RentalManager rentalManager;

    // ------------------------------------------------------------------ create
    @Test
    void create_shouldUseEmailToResolveUserId_notRequestField() {
        // Kullanıcı request body'de userId gönderse bile email'den türetilmeli
        CreateRentalRequest request = new CreateRentalRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5)
        );
        String email = "user@veyra.com";
        Long resolvedUserId = 42L;

        Car car = Car.builder()
                .dailyPrice(BigDecimal.valueOf(100))
                .status(CarStatus.AVAILABLE)
                .build();

        when(carRules.getByIdOrThrowForUpdate(1L)).thenReturn(car);
        when(userRules.getUserIdByEmail(email)).thenReturn(resolvedUserId);
        when(rentalMapper.toResponse(any())).thenReturn(RentalResponse.builder().userId(resolvedUserId).build());

        RentalResponse response = rentalManager.create(request, email);

        // userId her zaman email'den çözümlenmeli
        verify(userRules).getUserIdByEmail(email);
        assertThat(response.getUserId()).isEqualTo(resolvedUserId);
    }

    // ------------------------------------------------------------------ cancel
    @Test
    void cancel_shouldThrowForbidden_whenUserTriesToCancelSomeoneElsesRental() {
        Long rentalId  = 10L;
        Long ownerId   = 1L;
        Long attackerId = 99L;

        Rental rental = Rental.builder()
                .userId(ownerId)
                .carId(5L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(userRules.getUserIdByEmail("attacker@veyra.com")).thenReturn(attackerId);

        assertThatThrownBy(() ->
                rentalManager.cancel(rentalId, "attacker@veyra.com", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("size ait değil");

        // Rental asla değiştirilmemeli
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void cancel_shouldSucceed_whenUserCancelsOwnRental() {
        Long rentalId = 10L;
        Long userId   = 1L;

        Rental rental = Rental.builder()
                .userId(userId)
                .carId(5L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        Car car = Car.builder().status(CarStatus.RENTED).build();

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(userRules.getUserIdByEmail("owner@veyra.com")).thenReturn(userId);
        when(carRules.getByIdOrThrow(rental.getCarId())).thenReturn(car);
        when(rentalMapper.toResponse(any())).thenReturn(RentalResponse.builder().build());

        rentalManager.cancel(rentalId, "owner@veyra.com", false);

        verify(rentalRepository).save(rental);
        verify(carRepository).save(car);
        assertThat(rental.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(car.getStatus()).isEqualTo(CarStatus.AVAILABLE);
    }

    @Test
    void cancel_shouldSucceed_whenAdminCancelsSomeoneElsesRental() {
        Long rentalId = 10L;
        Long ownerId  = 1L;

        Rental rental = Rental.builder()
                .userId(ownerId)
                .carId(5L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(3))
                .totalPrice(BigDecimal.valueOf(300))
                .build();

        Car car = Car.builder().status(CarStatus.RENTED).build();

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(carRules.getByIdOrThrow(rental.getCarId())).thenReturn(car);
        when(rentalMapper.toResponse(any())).thenReturn(RentalResponse.builder().build());

        // isAdmin=true — ownership kontrolü atlanmalı
        rentalManager.cancel(rentalId, "admin@veyra.com", true);

        verify(userRules, never()).getUserIdByEmail(any());
        verify(rentalRepository).save(rental);
    }

    // ------------------------------------------------------------------ getById
    @Test
    void getById_shouldThrowForbidden_whenUserViewsSomeoneElsesRental() {
        Long rentalId   = 20L;
        Long ownerId    = 1L;
        Long attackerId = 77L;

        Rental rental = Rental.builder()
                .userId(ownerId)
                .carId(3L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(userRules.getUserIdByEmail("attacker@veyra.com")).thenReturn(attackerId);

        assertThatThrownBy(() ->
                rentalManager.getById(rentalId, "attacker@veyra.com", false))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("size ait değil");
    }

    @Test
    void getById_shouldSucceed_whenAdminViewsAnyRental() {
        Long rentalId = 20L;

        Rental rental = Rental.builder()
                .userId(1L)
                .carId(3L)
                .status(RentalStatus.ACTIVE)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .totalPrice(BigDecimal.valueOf(200))
                .build();

        when(rentalRules.getByIdOrThrow(rentalId)).thenReturn(rental);
        when(rentalMapper.toResponse(rental)).thenReturn(RentalResponse.builder().build());

        rentalManager.getById(rentalId, "admin@veyra.com", true);

        // Admin için getUserIdByEmail çağrılmamalı
        verify(userRules, never()).getUserIdByEmail(any());
    }
}
