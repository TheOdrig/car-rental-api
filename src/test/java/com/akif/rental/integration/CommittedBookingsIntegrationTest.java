package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.api.CarBooking;
import com.akif.car.api.CarBookingCalendar;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.e2e.infrastructure.TestFixtures;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Committed bookings for demand pricing")
class CommittedBookingsIntegrationTest extends E2ETestBase {

    private static final LocalDate START = LocalDate.of(2031, 3, 10);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private CarBookingCalendar bookingCalendar;

    private Rental rentalOf(User user, Car car, RentalStatus status, LocalDate start, LocalDate end) {
        return rentalRepository.save(Rental.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getUsername())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .startDate(start)
                .endDate(end)
                .days((int) (end.toEpochDay() - start.toEpochDay()) + 1)
                .currency(TestFixtures.DEFAULT_CURRENCY)
                .dailyPrice(car.getPrice())
                .totalPrice(car.getPrice().multiply(BigDecimal.ONE))
                .status(status)
                .isDeleted(false)
                .build());
    }

    @Test
    @DisplayName("only confirmed and in-use rentals count, requested holds, cancelled and returned ones do not")
    void onlyCommittedRentalsCount() {
        User user = userRepository.save(TestDataBuilder.createTestUser());
        Car confirmedCar = carRepository.save(TestDataBuilder.createAvailableCar());
        Car inUseCar = carRepository.save(TestDataBuilder.createAvailableCar());
        Car requestedCar = carRepository.save(TestDataBuilder.createAvailableCar());
        Car cancelledCar = carRepository.save(TestDataBuilder.createAvailableCar());
        Car returnedCar = carRepository.save(TestDataBuilder.createAvailableCar());
        rentalOf(user, confirmedCar, RentalStatus.CONFIRMED, START, START.plusDays(2));
        rentalOf(user, inUseCar, RentalStatus.IN_USE, START.minusDays(1), START.plusDays(1));
        rentalOf(user, requestedCar, RentalStatus.REQUESTED, START, START.plusDays(2));
        rentalOf(user, cancelledCar, RentalStatus.CANCELLED, START, START.plusDays(2));
        rentalOf(user, returnedCar, RentalStatus.RETURNED, START, START.plusDays(2));

        List<CarBooking> bookings = bookingCalendar.findCommittedBookings(START, START.plusDays(5));

        assertThat(bookings).extracting(CarBooking::carId)
                .containsExactlyInAnyOrder(confirmedCar.getId(), inUseCar.getId());
    }

    @Test
    @DisplayName("a rental outside the window is not returned")
    void rentalsOutsideTheWindowAreIgnored() {
        User user = userRepository.save(TestDataBuilder.createTestUser());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        rentalOf(user, car, RentalStatus.CONFIRMED, START.plusDays(10), START.plusDays(12));

        assertThat(bookingCalendar.findCommittedBookings(START, START.plusDays(5))).isEmpty();
    }
}
