package com.akif.rental.unit;

import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.RentalRequestExpiryScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RentalRequestExpiryScheduler cutoff")
class RentalRequestExpirySchedulerUnitTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC);
    private final RentalRepository repository = mock(RentalRepository.class);
    private final RentalProperties properties = new RentalProperties();
    private final org.springframework.context.ApplicationEventPublisher publisher =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    private final RentalRequestExpiryScheduler scheduler =
            new RentalRequestExpiryScheduler(clock, repository, properties, publisher);

    @Test
    @DisplayName("derives the cutoff from the injected clock and the configured hours")
    void cutoffUsesClockAndProperty() {
        properties.setRequestExpiryHours(10);
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of());

        scheduler.expireStaleRequests();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).findStaleRequests(org.mockito.ArgumentMatchers.eq(RentalStatus.REQUESTED), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 18, 2, 0));
    }

    @Test
    @DisplayName("cancels each stale request with the expiry reason")
    void cancelsStaleRequests() {
        Rental stale = Rental.builder().status(RentalStatus.REQUESTED).build();
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of(stale));

        int expired = scheduler.expireStaleRequests();

        assertThat(expired).isEqualTo(1);
        assertThat(stale.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(stale.getCancellationReason()).isEqualTo("Request expired without approval");
        verify(repository).save(stale);
    }

    @Test
    @DisplayName("does nothing when no request is stale")
    void noopWhenNothingStale() {
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of());

        assertThat(scheduler.expireStaleRequests()).isZero();
        verify(repository, never()).save(any());
        verify(publisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("publishes the normal cancellation event for every expired request so the customer is notified")
    void publishesCancellationEvent() {
        Rental stale = Rental.builder().status(RentalStatus.REQUESTED).userEmail("customer@example.com").build();
        stale.setId(77L);
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of(stale));

        scheduler.expireStaleRequests();

        ArgumentCaptor<com.akif.rental.api.RentalCancelledEvent> event =
                ArgumentCaptor.forClass(com.akif.rental.api.RentalCancelledEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().rentalId()).isEqualTo(77L);
        assertThat(event.getValue().customerEmail()).isEqualTo("customer@example.com");
        assertThat(event.getValue().cancellationReason()).contains("Request expired without approval");
        assertThat(event.getValue().refundProcessed()).isFalse();
        assertThat(event.getValue().refundAmount()).isEqualByComparingTo("0");
        assertThat(event.getValue().occurredAt()).isEqualTo(LocalDateTime.of(2026, 11, 18, 12, 0));
    }

    @Test
    @DisplayName("publishes one event per expired request")
    void publishesOneEventPerRequest() {
        Rental first = Rental.builder().status(RentalStatus.REQUESTED).build();
        Rental second = Rental.builder().status(RentalStatus.REQUESTED).build();
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of(first, second));

        scheduler.expireStaleRequests();

        verify(publisher, org.mockito.Mockito.times(2)).publishEvent(any(com.akif.rental.api.RentalCancelledEvent.class));
    }
}
