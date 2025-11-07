package com.akif.repository;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.enums.RentalStatus;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ContextConfiguration(classes = CarGalleryProjectApplication.class)
@ActiveProfiles("test")
@DisplayName("RentalRepository Integration Tests")
class RentalRepositoryTest {

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Car testCar;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(com.akif.enums.Role.USER))
                .enabled(true)
                .isDeleted(false)
                .build();
        testUser = userRepository.save(testUser);

        testCar = Car.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .isDeleted(false)
                .build();
        testCar = carRepository.save(testCar);
    }

    private Rental createRental(RentalStatus status, LocalDate startDate, LocalDate endDate, boolean isDeleted) {
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(startDate)
                .endDate(endDate)
                .days(days)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("500.00").multiply(BigDecimal.valueOf(days)))
                .currency(CurrencyType.TRY)
                .status(status)
                .isDeleted(isDeleted)
                .build();
    }

    @Nested
    @DisplayName("findByUserId Tests")
    class FindByUserIdTests {

        @Test
        @DisplayName("Should find rentals by user ID")
        void shouldFindRentalsByUserId() {
            Rental rental = createRental(
                    RentalStatus.REQUESTED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(rental);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> result = rentalRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUser().getId()).isEqualTo(testUser.getId());
        }

        @Test
        @DisplayName("Should not return deleted rentals")
        void shouldNotReturnDeletedRentals() {
            Rental rental = createRental(
                    RentalStatus.REQUESTED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    true
            );
            rentalRepository.save(rental);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> result = rentalRepository.findByUserIdAndIsDeletedFalse(testUser.getId(), pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("findByCarId Tests")
    class FindByCarIdTests {

        @Test
        @DisplayName("Should find rentals by car ID")
        void shouldFindRentalsByCarId() {
            Rental rental = createRental(
                    RentalStatus.REQUESTED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(rental);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> result = rentalRepository.findByCarIdAndIsDeletedFalse(testCar.getId(), pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCar().getId()).isEqualTo(testCar.getId());
        }
    }

    @Nested
    @DisplayName("countOverlappingRentals Tests")
    class CountOverlappingRentalsTests {

        @Test
        @DisplayName("Should return 0 when no overlapping rentals exist")
        void shouldReturnZeroWhenNoOverlappingRentalsExist() {
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(5);

            long count = rentalRepository.countOverlappingRentals(testCar.getId(), startDate, endDate);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should count overlapping CONFIRMED rentals")
        void shouldCountOverlappingConfirmedRentals() {
            Rental existingRental = createRental(
                    RentalStatus.CONFIRMED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(7);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count overlapping IN_USE rentals")
        void shouldCountOverlappingInUseRentals() {
            Rental existingRental = createRental(
                    RentalStatus.IN_USE,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(7);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not count REQUESTED rentals")
        void shouldNotCountRequestedRentals() {
            Rental existingRental = createRental(
                    RentalStatus.REQUESTED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(7);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should not count RETURNED rentals")
        void shouldNotCountReturnedRentals() {
            Rental existingRental = createRental(
                    RentalStatus.RETURNED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(7);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isZero();
        }

        @Test
        @DisplayName("Should detect exact overlap")
        void shouldDetectExactOverlap() {
            Rental existingRental = createRental(
                    RentalStatus.CONFIRMED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(1);
            LocalDate newEndDate = LocalDate.now().plusDays(5);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect partial overlap - new starts before existing ends")
        void shouldDetectPartialOverlapNewStartsBefore() {
            Rental existingRental = createRental(
                    RentalStatus.CONFIRMED,
                    LocalDate.now().plusDays(5),
                    LocalDate.now().plusDays(10),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(8);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should detect partial overlap - new starts after existing starts")
        void shouldDetectPartialOverlapNewStartsAfter() {
            Rental existingRental = createRental(
                    RentalStatus.CONFIRMED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(3);
            LocalDate newEndDate = LocalDate.now().plusDays(7);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not detect overlap when dates are adjacent")
        void shouldNotDetectOverlapWhenDatesAreAdjacent() {
            Rental existingRental = createRental(
                    RentalStatus.CONFIRMED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(existingRental);

            LocalDate newStartDate = LocalDate.now().plusDays(6);
            LocalDate newEndDate = LocalDate.now().plusDays(10);

            long count = rentalRepository.countOverlappingRentals(
                    testCar.getId(), newStartDate, newEndDate);

            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("findByStatus Tests")
    class FindByStatusTests {

        @Test
        @DisplayName("Should find rentals by status")
        void shouldFindRentalsByStatus() {
            Rental rental = createRental(
                    RentalStatus.REQUESTED,
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(5),
                    false
            );
            rentalRepository.save(rental);

            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> result = rentalRepository.findByStatusAndIsDeletedFalse(
                    RentalStatus.REQUESTED, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(RentalStatus.REQUESTED);
        }
    }
}