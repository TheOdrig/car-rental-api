package com.akif.rental.scheduler;

import java.time.Duration;
import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.RentalRequestExpiryScheduler;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Stale rental request expiry")
class RentalRequestExpirySchedulerTest {

    @Autowired
    private RentalRequestExpiryScheduler scheduler;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalProperties properties;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    private User user;

    @BeforeEach
    void setUp() {
        cleanUp();
        user = userRepository.save(TestDataBuilder.createTestUser("expiry-user"));
    }

    @AfterEach
    void cleanUp() {
        properties.setRequestExpiryHours(24);
        rentalRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM car_rental.payments");
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Rental rental(RentalStatus status, Instant createdAt, int offsetDays) {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate start = LocalDate.now().plusDays(10 + offsetDays);
        Rental saved = rentalRepository.save(
                TestDataBuilder.createRental(car, user, start, start.plusDays(3), status));
        jdbcTemplate.update("UPDATE car_rental.rentals SET create_time = ? WHERE id = ?", java.sql.Timestamp.from(createdAt), saved.getId());
        return saved;
    }

    private RentalStatus statusOf(Rental rental) {
        return rentalRepository.findById(rental.getId()).orElseThrow().getStatus();
    }

    @Test
    @DisplayName("cancels requests older than the configured window and leaves the rest alone")
    void cancelsOnlyStaleRequests() {
        Instant now = Instant.now(clock);
        Rental stale = rental(RentalStatus.REQUESTED, now.minus(Duration.ofHours(40)), 0);
        Rental fresh = rental(RentalStatus.REQUESTED, now.minus(Duration.ofHours(10)), 1);
        Rental oldConfirmed = rental(RentalStatus.CONFIRMED, now.minus(Duration.ofHours(72)), 2);

        int expired = scheduler.expireStaleRequests();

        assertThat(expired).isEqualTo(1);
        assertThat(statusOf(stale)).isEqualTo(RentalStatus.CANCELLED);
        assertThat(rentalRepository.findById(stale.getId()).orElseThrow().getCancellationReason())
                .isEqualTo("Request expired without approval");
        assertThat(statusOf(fresh)).isEqualTo(RentalStatus.REQUESTED);
        assertThat(statusOf(oldConfirmed)).isEqualTo(RentalStatus.CONFIRMED);
    }

    @Test
    @DisplayName("uses the configured expiry window")
    void usesConfiguredWindow() {
        properties.setRequestExpiryHours(6);
        Instant now = Instant.now(clock);
        Rental twelveHoursOld = rental(RentalStatus.REQUESTED, now.minus(Duration.ofHours(12)), 0);
        Rental oneHourOld = rental(RentalStatus.REQUESTED, now.minus(Duration.ofHours(1)), 1);

        scheduler.expireStaleRequests();

        assertThat(statusOf(twelveHoursOld)).isEqualTo(RentalStatus.CANCELLED);
        assertThat(statusOf(oneHourOld)).isEqualTo(RentalStatus.REQUESTED);
    }

    @Test
    @DisplayName("an expired request frees its dates for another booking")
    void expiredRequestFreesDates() {
        Rental stale = rental(RentalStatus.REQUESTED, Instant.now(clock).minus(Duration.ofHours(30)), 0);

        scheduler.expireStaleRequests();

        assertThat(rentalRepository.countOverlappingRentals(
                stale.getCarId(), stale.getStartDate(), stale.getEndDate())).isZero();
    }
}
