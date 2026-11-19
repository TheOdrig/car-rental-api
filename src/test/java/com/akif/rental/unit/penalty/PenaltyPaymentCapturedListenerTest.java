package com.akif.rental.unit.penalty;

import com.akif.payment.api.PaymentDto;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.PenaltyPaymentCapturedEvent;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.listener.PenaltyPaymentCapturedListener;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PenaltyPaymentCapturedListener")
class PenaltyPaymentCapturedListenerTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PenaltyPaymentCapturedListener listener;

    private final PenaltyPaymentCapturedEvent event = new PenaltyPaymentCapturedEvent(
            5L, 1L, new BigDecimal("1500.00"), CurrencyType.TRY, LocalDateTime.now());

    private Rental rentalWithPenalty(String penalty) {
        Rental rental = Rental.builder().id(1L).penaltyAmount(new BigDecimal(penalty)).penaltyPaid(false).build();
        when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(rental));
        return rental;
    }

    private void paymentIs(PaymentStatus status) {
        PaymentDto dto = new PaymentDto(5L, 1L, "a@b.c", "34ABC123", new BigDecimal("1500.00"), CurrencyType.TRY,
                status, "PENALTY", "pi_1", null, "pi_1", null);
        when(paymentService.getPaymentById(5L)).thenReturn(Optional.of(dto));
    }

    @Test
    @DisplayName("a captured payment of an outstanding penalty marks it paid")
    void marksAnOutstandingPenaltyPaid() {
        Rental rental = rentalWithPenalty("1500.00");
        paymentIs(PaymentStatus.CAPTURED);

        listener.onPenaltyPaymentCaptured(event);

        assertThat(rental.getPenaltyPaid()).isTrue();
        verify(rentalRepository).save(rental);
    }

    @Test
    @DisplayName("a fully waived penalty is not marked paid by a late capture")
    void ignoresAWaivedPenalty() {
        Rental rental = rentalWithPenalty("0.00");

        listener.onPenaltyPaymentCaptured(event);

        assertThat(rental.getPenaltyPaid()).isFalse();
        verify(rentalRepository, never()).save(rental);
    }

    @Test
    @DisplayName("a refunded payment does not mark the penalty paid")
    void ignoresARefundedPayment() {
        Rental rental = rentalWithPenalty("1500.00");
        paymentIs(PaymentStatus.REFUNDED);

        listener.onPenaltyPaymentCaptured(event);

        assertThat(rental.getPenaltyPaid()).isFalse();
        verify(rentalRepository, never()).save(rental);
    }
}
