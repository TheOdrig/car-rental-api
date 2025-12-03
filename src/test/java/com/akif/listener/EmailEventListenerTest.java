package com.akif.listener;

import com.akif.enums.CurrencyType;
import com.akif.event.*;
import com.akif.service.email.IEmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailEventListener Tests")
class EmailEventListenerTest {

    @Mock
    private IEmailNotificationService emailNotificationService;

    @InjectMocks
    private EmailEventListener emailEventListener;

    @Nested
    @DisplayName("Rental Confirmed Event")
    class RentalConfirmedEventTests {

        private RentalConfirmedEvent event;

        @BeforeEach
        void setUp() {
            event = new RentalConfirmedEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                "Toyota",
                "Corolla",
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2025, 12, 15),
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "Istanbul Airport"
            );
        }

        @Test
        @DisplayName("Should trigger sendRentalConfirmation when RentalConfirmedEvent is received")
        void shouldTriggerSendRentalConfirmationWhenEventReceived() {
            emailEventListener.handleRentalConfirmed(event);

            verify(emailNotificationService).sendRentalConfirmation(event);
        }
    }

    @Nested
    @DisplayName("Payment Captured Event")
    class PaymentCapturedEventTests {

        private PaymentCapturedEvent event;

        @BeforeEach
        void setUp() {
            event = new PaymentCapturedEvent(
                this,
                100L,
                1L,
                "customer@example.com",
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "TXN-123456",
                LocalDateTime.of(2025, 12, 5, 14, 30)
            );
        }

        @Test
        @DisplayName("Should trigger sendPaymentReceipt when PaymentCapturedEvent is received")
        void shouldTriggerSendPaymentReceiptWhenEventReceived() {
            emailEventListener.handlePaymentCaptured(event);

            verify(emailNotificationService).sendPaymentReceipt(event);
        }
    }

    @Nested
    @DisplayName("Pickup Reminder Event")
    class PickupReminderEventTests {

        private PickupReminderEvent event;

        @BeforeEach
        void setUp() {
            event = new PickupReminderEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDate.of(2025, 12, 10),
                "Istanbul Airport",
                "Toyota",
                "Corolla"
            );
        }

        @Test
        @DisplayName("Should trigger sendPickupReminder when PickupReminderEvent is received")
        void shouldTriggerSendPickupReminderWhenEventReceived() {
            emailEventListener.handlePickupReminder(event);

            verify(emailNotificationService).sendPickupReminder(event);
        }
    }

    @Nested
    @DisplayName("Return Reminder Event")
    class ReturnReminderEventTests {

        private ReturnReminderEvent event;

        @BeforeEach
        void setUp() {
            event = new ReturnReminderEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDate.of(2025, 12, 15),
                "Istanbul Airport",
                new BigDecimal("100.00")
            );
        }

        @Test
        @DisplayName("Should trigger sendReturnReminder when ReturnReminderEvent is received")
        void shouldTriggerSendReturnReminderWhenEventReceived() {
            emailEventListener.handleReturnReminder(event);

            verify(emailNotificationService).sendReturnReminder(event);
        }
    }

    @Nested
    @DisplayName("Rental Cancelled Event")
    class RentalCancelledEventTests {
        private RentalCancelledEvent event;

        @BeforeEach
        void setUp() {
            event = new RentalCancelledEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                LocalDateTime.of(2025, 12, 5, 10, 0),
                "Customer request",
                true,
                new BigDecimal("2500.00"),
                "REFUND-123456"
            );
        }

        @Test
        @DisplayName("Should trigger sendCancellationConfirmation when RentalCancelledEvent is received")
        void shouldTriggerSendCancellationConfirmationWhenEventReceived() {

            emailEventListener.handleRentalCancelled(event);

            verify(emailNotificationService).sendCancellationConfirmation(event);
        }
    }

    @Nested
    @DisplayName("Multiple Events")
    class MultipleEventsTests {

        @Test
        @DisplayName("Should handle multiple events independently")
        void shouldHandleMultipleEventsIndependently() {
            RentalConfirmedEvent confirmedEvent = new RentalConfirmedEvent(
                this,
                1L,
                "customer@example.com",
                LocalDateTime.now(),
                "Toyota",
                "Corolla",
                LocalDate.of(2025, 12, 10),
                LocalDate.of(2025, 12, 15),
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "Istanbul Airport"
            );

            PaymentCapturedEvent paymentEvent = new PaymentCapturedEvent(
                this,
                100L,
                1L,
                "customer@example.com",
                new BigDecimal("2500.00"),
                CurrencyType.TRY,
                "TXN-123456",
                LocalDateTime.of(2025, 12, 5, 14, 30)
            );

            emailEventListener.handleRentalConfirmed(confirmedEvent);
            emailEventListener.handlePaymentCaptured(paymentEvent);

            verify(emailNotificationService).sendRentalConfirmation(confirmedEvent);
            verify(emailNotificationService).sendPaymentReceipt(paymentEvent);
        }
    }
}
