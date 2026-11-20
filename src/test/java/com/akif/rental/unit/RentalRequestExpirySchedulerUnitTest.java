package com.akif.rental.unit;

import java.time.LocalDateTime;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.RentalProperties;
import com.akif.testsupport.ConfigFixtures;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.scheduler.RentalRequestExpiryScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
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
        private final org.springframework.context.ApplicationEventPublisher publisher =
            mock(org.springframework.context.ApplicationEventPublisher.class);
    private final RentalRequestExpiryScheduler scheduler =
            new RentalRequestExpiryScheduler(clock, repository, ConfigFixtures.rentalPropertiesWithRequestExpiryHours(10), publisher,
                    mock(org.springframework.transaction.PlatformTransactionManager.class));

    @Test
    @DisplayName("derives the cutoff from the injected clock and the configured hours")
    void cutoffUsesClockAndProperty() {
        when(repository.findStaleRequests(any(), any())).thenReturn(List.of());

        scheduler.expireStaleRequests();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(repository).findStaleRequests(org.mockito.ArgumentMatchers.eq(RentalStatus.REQUESTED), cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(LocalDateTime.of(2026, 11, 18, 2, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("cancels each stale request with the expiry reason")
    void cancelsStaleRequests() {
        Rental stale = staleRental(5L);

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
        staleRental(77L, RentalStatus.REQUESTED, "customer@example.com");

        scheduler.expireStaleRequests();

        ArgumentCaptor<com.akif.rental.api.RentalCancelledEvent> event =
                ArgumentCaptor.forClass(com.akif.rental.api.RentalCancelledEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().rentalId()).isEqualTo(77L);
        assertThat(event.getValue().customerEmail()).isEqualTo("customer@example.com");
        assertThat(event.getValue().cancellationReason()).contains("Request expired without approval");
        assertThat(event.getValue().refundProcessed()).isFalse();
        assertThat(event.getValue().refundAmount()).isEqualByComparingTo("0");
        assertThat(event.getValue().occurredAt()).isEqualTo(LocalDateTime.of(2026, 11, 18, 12, 0).toInstant(ZoneOffset.UTC));
    }

    @Test
    @DisplayName("publishes one event per expired request")
    void publishesOneEventPerRequest() {
        staleRental(1L);
        staleRental(2L);

        scheduler.expireStaleRequests();

        verify(publisher, org.mockito.Mockito.times(2)).publishEvent(any(com.akif.rental.api.RentalCancelledEvent.class));
    }

    @Test
    @DisplayName("a failure on one request does not stop the others")
    void failureDoesNotStopTheBatch() {
        Rental broken = staleRental(10L);
        Rental healthy = staleRental(11L);
        when(repository.findByIdForUpdate(10L)).thenThrow(new RuntimeException("optimistic lock"));

        int expired = scheduler.expireStaleRequests();

        assertThat(expired).isEqualTo(1);
        assertThat(broken.getStatus()).isEqualTo(RentalStatus.REQUESTED);
        assertThat(healthy.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        verify(publisher).publishEvent(any(com.akif.rental.api.RentalCancelledEvent.class));
    }

    @Test
    @DisplayName("skips a request that is no longer in REQUESTED once locked")
    void skipsRequestThatChangedState() {
        staleRental(20L, RentalStatus.CONFIRMED, null);

        assertThat(scheduler.expireStaleRequests()).isZero();
        verify(repository, never()).save(any());
    }

    private Rental staleRental(Long id) {
        return staleRental(id, RentalStatus.REQUESTED, null);
    }

    private Rental staleRental(Long id, RentalStatus status, String email) {
        Rental rental = Rental.builder().status(status).userEmail(email).build();
        rental.setId(id);
        List<Rental> current = new java.util.ArrayList<>(
                repository.findStaleRequests(RentalStatus.REQUESTED, Instant.EPOCH));
        current.add(rental);
        when(repository.findStaleRequests(any(), any())).thenReturn(current);
        when(repository.findByIdForUpdate(id)).thenReturn(java.util.Optional.of(rental));
        return rental;
    }
}
