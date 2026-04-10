package com.veyra.rental.manager;

import com.veyra.core.response.PageResponse;
import com.veyra.core.util.SecurityUtils;
import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import com.veyra.rental.mapper.RentalMapper;
import com.veyra.rental.repository.RentalRepository;
import com.veyra.rental.rules.RentalRules;
import com.veyra.rental.service.RentalService;
import com.veyra.vehicle.car.rules.CarRules;
import com.veyra.vehicle.car.service.CarService;
import com.veyra.user.rules.UserRules;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalManager implements RentalService {

    private final RentalRepository rentalRepository;
    private final RentalRules      rentalRules;
    private final CarRules         carRules;
    private final CarService       carService;
    private final UserRules        userRules;
    private final RentalMapper     rentalMapper;

    @Override
    @Transactional
    public RentalResponse create(CreateRentalRequest request, String email) {
        rentalRules.checkIfDatesValid(request.getStartDate(), request.getEndDate());

        // Aynı araç için eş zamanlı istekleri önlemek adına satır kilidi alınır.
        // İkinci işlem bu commit'e kadar bu noktada bekler; commit'ten sonra
        // checkIfCarAvailable veya checkIfCarAlreadyRented hatasıyla dönerse sağlıklı 422 üretir.
        var car = carRules.getByIdOrThrowForUpdate(request.getCarId());
        carRules.checkIfCarAvailable(car);
        rentalRules.checkIfCarAlreadyRented(request.getCarId());

        Long userId = userRules.getUserIdByEmail(email);

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalPrice = car.getDailyPrice().multiply(BigDecimal.valueOf(days));

        var rental = Rental.builder()
                .carId(request.getCarId())
                .userId(userId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .build();

        rentalRepository.save(rental);

        carService.markAsRented(request.getCarId());

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalResponse complete(Long id) {
        var rental = rentalRules.getByIdOrThrow(id);
        rentalRules.checkIfRentalIsActive(rental);

        rental.setStatus(RentalStatus.COMPLETED);
        rentalRepository.save(rental);

        carService.markAsAvailable(rental.getCarId());

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalResponse cancel(Long id, String email, boolean isAdmin) {
        var rental = rentalRules.getByIdOrThrow(id);
        SecurityUtils.checkOwnership(rental.getUserId(), email, isAdmin, userRules::getUserIdByEmail);
        rentalRules.checkIfRentalIsActive(rental);

        rental.setStatus(RentalStatus.CANCELLED);
        rentalRepository.save(rental);

        carService.markAsAvailable(rental.getCarId());

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponse getById(Long id, String email, boolean isAdmin) {
        var rental = rentalRules.getByIdOrThrow(id);
        SecurityUtils.checkOwnership(rental.getUserId(), email, isAdmin, userRules::getUserIdByEmail);
        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalResponse> getAll() {
        return rentalRepository.findAll()
                .stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalResponse> getAll(Pageable pageable) {
        return new PageResponse<>(rentalRepository.findAll(pageable).map(rentalMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalResponse> getAllByUserId(Long userId) {
        userRules.checkIfUserExists(userId);
        return rentalRepository.findAllByUserId(userId)
                .stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalResponse> getAllByUserId(Long userId, Pageable pageable) {
        userRules.checkIfUserExists(userId);
        return new PageResponse<>(rentalRepository.findAllByUserId(userId, pageable)
                .map(rentalMapper::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RentalResponse> getMyRentals(String email) {
        Long userId = userRules.getUserIdByEmail(email);
        return rentalRepository.findAllByUserId(userId)
                .stream()
                .map(rentalMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RentalResponse> getMyRentals(String email, Pageable pageable) {
        Long userId = userRules.getUserIdByEmail(email);
        return new PageResponse<>(rentalRepository.findAllByUserId(userId, pageable)
                .map(rentalMapper::toResponse));
    }
}
