package com.veyra.rental.manager;

import com.veyra.rental.dto.request.CreateRentalRequest;
import com.veyra.rental.dto.response.RentalResponse;
import com.veyra.rental.entity.Rental;
import com.veyra.rental.enums.RentalStatus;
import com.veyra.rental.mapper.RentalMapper;
import com.veyra.rental.repository.RentalRepository;
import com.veyra.rental.rules.RentalRules;
import com.veyra.rental.service.RentalService;
import com.veyra.vehicle.car.enums.CarStatus;
import com.veyra.vehicle.car.rules.CarRules;
import com.veyra.vehicle.car.repository.CarRepository;
import com.veyra.user.rules.UserRules;
import lombok.RequiredArgsConstructor;
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
    private final CarRepository    carRepository;
    private final UserRules        userRules;
    private final RentalMapper     rentalMapper;

    @Override
    @Transactional
    public RentalResponse create(CreateRentalRequest request) {
        rentalRules.checkIfDatesValid(request.getStartDate(), request.getEndDate());

        var car = carRules.getByIdOrThrow(request.getCarId());
        carRules.checkIfCarAvailable(car);
        rentalRules.checkIfCarAlreadyRented(request.getCarId());

        userRules.checkIfUserExists(request.getUserId());

        long days = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalPrice = car.getDailyPrice().multiply(BigDecimal.valueOf(days));

        var rental = Rental.builder()
                .carId(request.getCarId())
                .userId(request.getUserId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalPrice(totalPrice)
                .build();

        rentalRepository.save(rental);

        // Araç durumunu RENTED yap
        car.setStatus(CarStatus.RENTED);
        carRepository.save(car);

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalResponse complete(Long id) {
        var rental = rentalRules.getByIdOrThrow(id);
        rentalRules.checkIfRentalIsActive(rental);

        rental.setStatus(RentalStatus.COMPLETED);
        rentalRepository.save(rental);

        // Araç durumunu AVAILABLE'a geri al
        var car = carRules.getByIdOrThrow(rental.getCarId());
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional
    public RentalResponse cancel(Long id) {
        var rental = rentalRules.getByIdOrThrow(id);
        rentalRules.checkIfRentalIsActive(rental);

        rental.setStatus(RentalStatus.CANCELLED);
        rentalRepository.save(rental);

        // İptal edilince araç tekrar AVAILABLE
        var car = carRules.getByIdOrThrow(rental.getCarId());
        car.setStatus(CarStatus.AVAILABLE);
        carRepository.save(car);

        return rentalMapper.toResponse(rental);
    }

    @Override
    @Transactional(readOnly = true)
    public RentalResponse getById(Long id) {
        return rentalMapper.toResponse(rentalRules.getByIdOrThrow(id));
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
    public List<RentalResponse> getAllByUserId(Long userId) {
        userRules.checkIfUserExists(userId);
        return rentalRepository.findAllByUserId(userId)
                .stream()
                .map(rentalMapper::toResponse)
                .toList();
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
}
