package com.akif.rental.scheduler;

import com.akif.rental.api.PickupReminderEvent;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.api.ReturnReminderEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.testsupport.ConfigFixtures;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.ReminderScheduler;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Reminder event content")
class ReminderEventContentTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T08:00:00Z"), ZoneOffset.UTC);

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private Rental rental(CurrencyType currency, String dailyPrice) {
        return Rental.builder()
                .id(1L)
                .userEmail("customer@example.com")
                .carBrand("Toyota")
                .carModel("Corolla")
                .startDate(LocalDate.of(2026, 3, 11))
                .endDate(LocalDate.of(2026, 3, 10))
                .currency(currency)
                .dailyPrice(new BigDecimal(dailyPrice))
                .status(RentalStatus.IN_USE)
                .build();
    }

    private ReminderScheduler scheduler(PenaltyConfig config) {
        return new ReminderScheduler(CLOCK, rentalRepository, config, eventPublisher,
                new com.akif.rental.internal.config.ReminderProperties(50, 200));
    }

    @Test
    @DisplayName("the return reminder carries the penalty derived from the penalty configuration and the rental currency")
    void returnReminderUsesPenaltyConfigurationAndCurrency() {
        PenaltyConfig config = ConfigFixtures.penaltyConfig(30, "0.20", "2.00", 24);
        when(rentalRepository.findRentalsForReturnReminder(eq(LocalDate.of(2026, 3, 10)), eq(RentalStatus.IN_USE),
                anyLong(), any(Pageable.class))).thenReturn(List.of(rental(CurrencyType.JPY, "5000")));

        scheduler(config).sendReturnReminders();

        ArgumentCaptor<ReturnReminderEvent> captor = ArgumentCaptor.forClass(ReturnReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ReturnReminderEvent event = captor.getValue();
        assertThat(event.currency()).isEqualTo(CurrencyType.JPY);
        assertThat(event.hourlyPenaltyAmount()).isEqualByComparingTo("1000");
        assertThat(event.dailyPenaltyAmount()).isEqualByComparingTo("10000");
        assertThat(event.gracePeriodMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("the pickup reminder names the car and the date and invents no location")
    void pickupReminderHasNoPlaceholderLocation() {
        when(rentalRepository.findRentalsForPickupReminder(eq(LocalDate.of(2026, 3, 11)), eq(RentalStatus.CONFIRMED),
                anyLong(), any(Pageable.class))).thenReturn(List.of(rental(CurrencyType.TRY, "100")));

        scheduler(ConfigFixtures.penaltyConfig()).sendPickupReminders();

        ArgumentCaptor<PickupReminderEvent> captor = ArgumentCaptor.forClass(PickupReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().pickupDate()).isEqualTo(LocalDate.of(2026, 3, 11));
        assertThat(captor.getValue().carBrand()).isEqualTo("Toyota");
        assertThat(PickupReminderEvent.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("pickupLocation");
    }
}
