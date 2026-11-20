package com.akif.rental.scheduler;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.repository.CarRepository;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;
import com.akif.rental.api.PickupReminderEvent;
import com.akif.rental.api.ReturnReminderEvent;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@TestPropertySource(properties = "rental.reminder.batch-size=2")
@RecordApplicationEvents
@Transactional
@DisplayName("ReminderScheduler paging")
class ReminderSchedulerPagingIntegrationTest {

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

    @Autowired
    private ApplicationEvents events;

    private User user;
    private int carSequence;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .username("pageuser")
                .email("page@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
    }

    @Test
    @DisplayName("reminds every due pickup exactly once across several pages")
    void remindsAllPickupsOnce() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ids.add(rentalRepository.save(rental(tomorrow, tomorrow.plusDays(3), RentalStatus.CONFIRMED)).getId());
        }

        reminderScheduler.sendPickupReminders();

        assertThat(ids).allSatisfy(id ->
                assertThat(rentalRepository.findById(id).orElseThrow().isPickupReminderSent()).isTrue());
        List<Long> reminded = events.stream(PickupReminderEvent.class).map(PickupReminderEvent::rentalId).toList();
        assertThat(reminded).containsExactlyInAnyOrderElementsOf(ids);
    }

    @Test
    @DisplayName("reminds every due return exactly once across several pages")
    void remindsAllReturnsOnce() {
        LocalDate today = LocalDate.now();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            ids.add(rentalRepository.save(rental(today.minusDays(3), today, RentalStatus.IN_USE)).getId());
        }

        reminderScheduler.sendReturnReminders();

        assertThat(ids).allSatisfy(id ->
                assertThat(rentalRepository.findById(id).orElseThrow().isReturnReminderSent()).isTrue());
        List<Long> reminded = events.stream(ReturnReminderEvent.class).map(ReturnReminderEvent::rentalId).toList();
        assertThat(reminded).containsExactlyInAnyOrderElementsOf(ids);
    }

    private Rental rental(LocalDate start, LocalDate end, RentalStatus status) {
        Car car = carRepository.save(Car.builder()
                .licensePlate("34P" + (++carSequence))
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build());
        int days = (int) (end.toEpochDay() - start.toEpochDay());
        return Rental.builder()
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getUsername())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .startDate(start)
                .endDate(end)
                .days(days)
                .currency(CurrencyType.TRY)
                .dailyPrice(car.getPrice())
                .totalPrice(car.getPrice().multiply(new BigDecimal(days)))
                .status(status)
                .pickupReminderSent(false)
                .returnReminderSent(false)
                .build();
    }
}
