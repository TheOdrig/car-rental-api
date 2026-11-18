package com.akif.dashboard.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.dashboard.api.AlertService;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.internal.repository.RentalRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Fleet availability reflects rentals, not the fleet status flag")
class LowAvailabilityAlertIntegrationTest {

    @Autowired
    private AlertService alertService;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private DashboardQueryService dashboardQueryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User user;

    @BeforeEach
    void setUp() {
        cleanUp();
        user = userRepository.save(TestDataBuilder.createTestUser("fleet-user"));
    }

    @AfterEach
    void cleanUp() {
        alertRepository.deleteAll();
        rentalRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM car_rental.payments");
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("an all-AVAILABLE fleet that is fully rented out raises the low availability alert")
    void fullyRentedFleetRaisesAlert() {
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 5; i++) {
            Car car = carRepository.save(TestDataBuilder.createAvailableCar());
            rentalRepository.save(TestDataBuilder.createRental(
                    car, user, today.minusDays(1), today.plusDays(2), RentalStatus.IN_USE));
        }

        FleetStatusDto fleet = dashboardQueryService.fetchFleetStatus();
        alertService.generateAlerts();

        assertThat(fleet.totalCars()).isEqualTo(5);
        assertThat(fleet.rentedCars()).isEqualTo(5);
        assertThat(fleet.availableCars()).isZero();
        assertThat(alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                AlertType.LOW_AVAILABILITY, 0L)).isTrue();
    }

    @Test
    @DisplayName("several rentals on one car do not push occupancy past 100 percent")
    void severalRentalsOnOneCarCountOnce() {
        LocalDate today = LocalDate.now();
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        rentalRepository.save(TestDataBuilder.createRental(
                car, user, today.minusDays(10), today.minusDays(5), RentalStatus.IN_USE));
        rentalRepository.save(TestDataBuilder.createRental(
                car, user, today.plusDays(10), today.plusDays(15), RentalStatus.CONFIRMED));

        FleetStatusDto fleet = dashboardQueryService.fetchFleetStatus();

        assertThat(fleet.totalCars()).isEqualTo(1);
        assertThat(fleet.rentedCars()).isEqualTo(1);
        assertThat(fleet.occupancyRate().doubleValue()).isLessThanOrEqualTo(100.0);
    }
}
