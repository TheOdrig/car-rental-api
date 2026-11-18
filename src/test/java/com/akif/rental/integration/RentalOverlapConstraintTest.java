package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Rental overlap exclusion constraint")
class RentalOverlapConstraintTest {

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Car car;
    private Car otherCar;
    private User user;
    private final LocalDate base = LocalDate.now().plusDays(30);

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        car = carRepository.save(TestDataBuilder.createAvailableCar());
        otherCar = carRepository.save(TestDataBuilder.createAvailableCar());
        user = userRepository.save(TestDataBuilder.createTestUser());
    }

    @AfterEach
    void tearDown() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Rental rental(Car forCar, int fromOffset, int toOffset, RentalStatus status) {
        return TestDataBuilder.createRental(forCar, user, base.plusDays(fromOffset), base.plusDays(toOffset), status);
    }

    private static String sqlState(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException) {
                return sqlException.getSQLState();
            }
        }
        return null;
    }

    @Test
    @DisplayName("rejects overlapping active rentals of the same car with an exclusion violation")
    void rejectsOverlap() {
        rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.CONFIRMED));

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental(car, 3, 8, RentalStatus.REQUESTED)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(sqlState(error)).isEqualTo("23P01"));
    }

    @Test
    @DisplayName("treats the end date as part of the booking")
    void endDateIsInclusive() {
        rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.REQUESTED));

        assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental(car, 5, 9, RentalStatus.REQUESTED)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatCode(() -> rentalRepository.saveAndFlush(rental(car, 6, 9, RentalStatus.REQUESTED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("blocks every active status")
    void blocksEveryActiveStatus() {
        for (RentalStatus status : new RentalStatus[]{RentalStatus.REQUESTED, RentalStatus.CONFIRMED, RentalStatus.IN_USE}) {
            Car fresh = carRepository.save(TestDataBuilder.createAvailableCar());
            rentalRepository.saveAndFlush(rental(fresh, 0, 5, status));

            assertThatThrownBy(() -> rentalRepository.saveAndFlush(rental(fresh, 1, 2, RentalStatus.REQUESTED)))
                    .as("status %s", status)
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Test
    @DisplayName("ignores rentals that are no longer active")
    void ignoresInactiveRentals() {
        rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.CANCELLED));
        rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.RETURNED));

        assertThatCode(() -> rentalRepository.saveAndFlush(rental(car, 1, 4, RentalStatus.REQUESTED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ignores soft deleted rentals")
    void ignoresSoftDeletedRentals() {
        Rental deleted = rental(car, 0, 5, RentalStatus.CONFIRMED);
        deleted.softDelete();
        rentalRepository.saveAndFlush(deleted);

        assertThatCode(() -> rentalRepository.saveAndFlush(rental(car, 1, 4, RentalStatus.REQUESTED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lets different cars share the same dates")
    void differentCarsDoNotConflict() {
        rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.CONFIRMED));

        assertThatCode(() -> rentalRepository.saveAndFlush(rental(otherCar, 0, 5, RentalStatus.CONFIRMED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a cancelled booking frees the dates")
    void cancellationFreesDates() {
        Rental first = rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.REQUESTED));
        first.cancel("changed plans");
        rentalRepository.saveAndFlush(first);

        assertThatCode(() -> rentalRepository.saveAndFlush(rental(car, 0, 5, RentalStatus.REQUESTED)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the btree_gist extension is installed")
    void extensionInstalled() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'btree_gist'", Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
