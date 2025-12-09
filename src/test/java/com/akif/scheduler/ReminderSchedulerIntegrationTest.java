package com.akif.scheduler;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.RentalStatus;
import com.akif.shared.enums.Role;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ReminderScheduler Integration Tests")
class ReminderSchedulerIntegrationTest {

    @Autowired
    private ReminderScheduler reminderScheduler;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Car testCar;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("customer@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        testCar = Car.builder()
                .licensePlate("34TEST123")
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
                .build();
        testCar = carRepository.save(testCar);
    }

    @Nested
    @DisplayName("Pickup Reminder Tests")
    class PickupReminderTests {

        @Test
        @DisplayName("Should send pickup reminder for rentals with pickup date tomorrow")
        void shouldSendPickupReminderForTomorrowRentals() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Rental rental = createRental(tomorrow, tomorrow.plusDays(4), RentalStatus.CONFIRMED);
            rental.setPickupReminderSent(false);
            rental = rentalRepository.save(rental);

            reminderScheduler.sendPickupReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isPickupReminderSent()).isTrue();
        }

        @Test
        @DisplayName("Should not send pickup reminder for rentals with pickup date today")
        void shouldNotSendPickupReminderForTodayRentals() {
            LocalDate today = LocalDate.now();
            Rental rental = createRental(today, today.plusDays(4), RentalStatus.CONFIRMED);
            rental.setPickupReminderSent(false);
            rental = rentalRepository.save(rental);

            reminderScheduler.sendPickupReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isPickupReminderSent()).isFalse();
        }

        @Test
        @DisplayName("Should not send pickup reminder for rentals with status other than CONFIRMED")
        void shouldNotSendPickupReminderForNonConfirmedRentals() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Rental requestedRental = createRental(tomorrow, tomorrow.plusDays(4), RentalStatus.REQUESTED);
            requestedRental.setPickupReminderSent(false);
            requestedRental = rentalRepository.save(requestedRental);

            Rental inUseRental = createRental(tomorrow, tomorrow.plusDays(4), RentalStatus.IN_USE);
            inUseRental.setPickupReminderSent(false);
            inUseRental = rentalRepository.save(inUseRental);

            reminderScheduler.sendPickupReminders();

            Rental updatedRequested = rentalRepository.findById(requestedRental.getId()).orElseThrow();
            assertThat(updatedRequested.isPickupReminderSent()).isFalse();
            
            Rental updatedInUse = rentalRepository.findById(inUseRental.getId()).orElseThrow();
            assertThat(updatedInUse.isPickupReminderSent()).isFalse();
        }

        @Test
        @DisplayName("Should not send duplicate pickup reminders")
        void shouldNotSendDuplicatePickupReminders() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Rental rental = createRental(tomorrow, tomorrow.plusDays(4), RentalStatus.CONFIRMED);
            rental.setPickupReminderSent(true); // Already sent
            rental = rentalRepository.save(rental);

            reminderScheduler.sendPickupReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isPickupReminderSent()).isTrue();
        }

        @Test
        @DisplayName("Should send pickup reminders for multiple eligible rentals")
        void shouldSendPickupRemindersForMultipleRentals() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            
            Rental rental1 = createRental(tomorrow, tomorrow.plusDays(4), RentalStatus.CONFIRMED);
            rental1.setPickupReminderSent(false);
            rental1 = rentalRepository.save(rental1);

            Rental rental2 = createRental(tomorrow, tomorrow.plusDays(3), RentalStatus.CONFIRMED);
            rental2.setPickupReminderSent(false);
            rental2 = rentalRepository.save(rental2);

            reminderScheduler.sendPickupReminders();

            Rental updatedRental1 = rentalRepository.findById(rental1.getId()).orElseThrow();
            assertThat(updatedRental1.isPickupReminderSent()).isTrue();
            
            Rental updatedRental2 = rentalRepository.findById(rental2.getId()).orElseThrow();
            assertThat(updatedRental2.isPickupReminderSent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Return Reminder Tests")
    class ReturnReminderTests {

        @Test
        @DisplayName("Should send return reminder for rentals with return date today")
        void shouldSendReturnReminderForTodayReturns() {
            LocalDate today = LocalDate.now();
            Rental rental = createRental(today.minusDays(4), today, RentalStatus.IN_USE);
            rental.setReturnReminderSent(false);
            rental = rentalRepository.save(rental);

            reminderScheduler.sendReturnReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isReturnReminderSent()).isTrue();
        }

        @Test
        @DisplayName("Should not send return reminder for rentals with return date tomorrow")
        void shouldNotSendReturnReminderForTomorrowReturns() {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            Rental rental = createRental(tomorrow.minusDays(4), tomorrow, RentalStatus.IN_USE);
            rental.setReturnReminderSent(false);
            rental = rentalRepository.save(rental);

            reminderScheduler.sendReturnReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isReturnReminderSent()).isFalse();
        }

        @Test
        @DisplayName("Should not send return reminder for rentals with status other than IN_USE")
        void shouldNotSendReturnReminderForNonInUseRentals() {
            LocalDate today = LocalDate.now();
            Rental confirmedRental = createRental(today.minusDays(4), today, RentalStatus.CONFIRMED);
            confirmedRental.setReturnReminderSent(false);
            confirmedRental = rentalRepository.save(confirmedRental);

            Rental returnedRental = createRental(today.minusDays(4), today, RentalStatus.RETURNED);
            returnedRental.setReturnReminderSent(false);
            returnedRental = rentalRepository.save(returnedRental);

            reminderScheduler.sendReturnReminders();

            Rental updatedConfirmed = rentalRepository.findById(confirmedRental.getId()).orElseThrow();
            assertThat(updatedConfirmed.isReturnReminderSent()).isFalse();
            
            Rental updatedReturned = rentalRepository.findById(returnedRental.getId()).orElseThrow();
            assertThat(updatedReturned.isReturnReminderSent()).isFalse();
        }

        @Test
        @DisplayName("Should not send duplicate return reminders")
        void shouldNotSendDuplicateReturnReminders() {
            LocalDate today = LocalDate.now();
            Rental rental = createRental(today.minusDays(4), today, RentalStatus.IN_USE);
            rental.setReturnReminderSent(true); // Already sent
            rental = rentalRepository.save(rental);

            reminderScheduler.sendReturnReminders();

            Rental updatedRental = rentalRepository.findById(rental.getId()).orElseThrow();
            assertThat(updatedRental.isReturnReminderSent()).isTrue();
        }

        @Test
        @DisplayName("Should send return reminders for multiple eligible rentals")
        void shouldSendReturnRemindersForMultipleRentals() {
            LocalDate today = LocalDate.now();
            
            Rental rental1 = createRental(today.minusDays(4), today, RentalStatus.IN_USE);
            rental1.setReturnReminderSent(false);
            rental1 = rentalRepository.save(rental1);

            Rental rental2 = createRental(today.minusDays(3), today, RentalStatus.IN_USE);
            rental2.setReturnReminderSent(false);
            rental2 = rentalRepository.save(rental2);

            reminderScheduler.sendReturnReminders();

            Rental updatedRental1 = rentalRepository.findById(rental1.getId()).orElseThrow();
            assertThat(updatedRental1.isReturnReminderSent()).isTrue();
            
            Rental updatedRental2 = rentalRepository.findById(rental2.getId()).orElseThrow();
            assertThat(updatedRental2.isReturnReminderSent()).isTrue();
        }
    }

    private Rental createRental(LocalDate startDate, LocalDate endDate, RentalStatus status) {
        int days = (int) (endDate.toEpochDay() - startDate.toEpochDay());
        BigDecimal dailyPrice = testCar.getPrice();
        BigDecimal totalPrice = dailyPrice.multiply(new BigDecimal(days));

        return Rental.builder()
                .user(testUser)
                .car(testCar)
                .startDate(startDate)
                .endDate(endDate)
                .days(days)
                .currency(CurrencyType.TRY)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .status(status)
                .pickupReminderSent(false)
                .returnReminderSent(false)
                .build();
    }
}
