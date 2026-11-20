package com.akif.car.internal.service;

import com.akif.car.api.CarBookingCalendar;
import com.akif.car.api.CarCurrentRental;
import com.akif.car.api.CarOccupancy;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarStatusType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminFleetService {

    private final Clock clock;
    private final CarQueryService carQueryService;
    private final CarBookingCalendar bookingCalendar;

    public Page<CarResponse> search(String search, String brand, CarStatusType status, CarOccupancy occupancy,
                                    Pageable pageable) {
        Map<Long, CarCurrentRental> current = bookingCalendar.findCurrentRentals(LocalDate.now(clock));
        Page<CarResponse> page;
        if (occupancy == null) {
            page = carQueryService.searchForAdmin(search, brand, status, null, null, pageable);
        } else if (occupancy == CarOccupancy.FREE) {
            page = carQueryService.searchForAdmin(search, brand, status, "NOT_IN",
                    List.copyOf(current.keySet()), pageable);
        } else {
            List<Long> matching = current.values().stream()
                    .filter(rental -> rental.occupancy() == occupancy)
                    .map(CarCurrentRental::carId)
                    .toList();
            page = carQueryService.searchForAdmin(search, brand, status, "IN", matching, pageable);
        }
        return page.map(car -> withOccupancy(car, current));
    }

    public CarResponse get(Long id) {
        Map<Long, CarCurrentRental> current = bookingCalendar.findCurrentRentals(LocalDate.now(clock));
        return withOccupancy(carQueryService.getCarForAdmin(id), current);
    }

    private static CarResponse withOccupancy(CarResponse car, Map<Long, CarCurrentRental> current) {
        CarCurrentRental rental = current.get(car.id());
        if (rental == null) {
            return car.toBuilder().occupancy(CarOccupancy.FREE).build();
        }
        return car.toBuilder().occupancy(rental.occupancy()).currentRentalId(rental.rentalId()).build();
    }
}
