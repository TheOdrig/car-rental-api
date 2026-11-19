package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Bookable fleet query")
class BookableCarIdsRepositoryIntegrationTest {

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @BeforeEach
    @AfterEach
    void cleanUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
    }

    @Test
    @DisplayName("Excludes soft-deleted cars and cars in unavailable statuses")
    void excludesDeletedAndUnavailable() {
        Car live = carRepository.save(TestDataBuilder.createAvailableCar());

        Car deleted = carRepository.save(TestDataBuilder.createAvailableCar());
        deleted.softDelete();
        carRepository.save(deleted);

        Car maintenance = TestDataBuilder.createAvailableCar();
        maintenance.setCarStatusType(CarStatusType.MAINTENANCE);
        carRepository.save(maintenance);

        List<Long> ids = carRepository.findBookableCarIds(Arrays.asList(CarStatusType.getUnavailableStatuses()));

        assertThat(ids).containsExactly(live.getId());
    }
}
