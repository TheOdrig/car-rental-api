package com.akif.service.reconciliation;

import com.akif.dto.reconciliation.Discrepancy;
import com.akif.dto.reconciliation.StripePayment;
import com.akif.shared.enums.DiscrepancyType;
import com.akif.shared.enums.PaymentStatus;
import com.akif.model.Payment;
import com.akif.repository.PaymentRepository;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationService Tests")
class PaymentReconciliationServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentReconciliationService reconciliationService;

    @Nested
    @DisplayName("Discrepancy Detection Tests")
    class DiscrepancyDetectionTests {

        @Test
        @DisplayName("Should detect payment missing in Stripe")
        void shouldDetectPaymentMissingInStripe() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            List<Payment> dbPayments = List.of(dbPayment);
            List<StripePayment> stripePayments = new ArrayList<>();

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.MISSING_IN_STRIPE);
            assertThat(discrepancies.get(0).paymentId()).isEqualTo("1");
            assertThat(discrepancies.get(0).stripePaymentIntentId()).isEqualTo("pi_123");
        }

        @Test
        @DisplayName("Should detect payment missing in database")
        void shouldDetectPaymentMissingInDatabase() {
            List<Payment> dbPayments = new ArrayList<>();
            StripePayment stripePayment = new StripePayment(
                    "ch_123",
                    "pi_456",
                    new BigDecimal("100.00"),
                    "USD",
                    "succeeded"
            );
            List<StripePayment> stripePayments = List.of(stripePayment);

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.MISSING_IN_DATABASE);
            assertThat(discrepancies.get(0).stripePaymentIntentId()).isEqualTo("pi_456");
        }

        @Test
        @DisplayName("Should detect amount mismatch")
        void shouldDetectAmountMismatch() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            List<Payment> dbPayments = List.of(dbPayment);

            StripePayment stripePayment = new StripePayment(
                    "ch_123",
                    "pi_123",
                    new BigDecimal("150.00"),
                    "USD",
                    "succeeded"
            );
            List<StripePayment> stripePayments = List.of(stripePayment);

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.AMOUNT_MISMATCH);
            assertThat(discrepancies.get(0).databaseAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(discrepancies.get(0).stripeAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should detect status mismatch")
        void shouldDetectStatusMismatch() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.PENDING);
            List<Payment> dbPayments = List.of(dbPayment);

            StripePayment stripePayment = new StripePayment(
                    "ch_123",
                    "pi_123",
                    new BigDecimal("100.00"),
                    "USD",
                    "succeeded"
            );
            List<StripePayment> stripePayments = List.of(stripePayment);

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.STATUS_MISMATCH);
            assertThat(discrepancies.get(0).databaseStatus()).isEqualTo("PENDING");
            assertThat(discrepancies.get(0).stripeStatus()).isEqualTo("succeeded");
        }

        @Test
        @DisplayName("Should not report discrepancy for matching payments")
        void shouldNotReportDiscrepancyForMatchingPayments() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            List<Payment> dbPayments = List.of(dbPayment);

            StripePayment stripePayment = new StripePayment(
                    "ch_123",
                    "pi_123",
                    new BigDecimal("100.00"),
                    "USD",
                    "succeeded"
            );
            List<StripePayment> stripePayments = List.of(stripePayment);

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple discrepancies")
        void shouldHandleMultipleDiscrepancies() {
            Payment dbPayment1 = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            Payment dbPayment2 = createPayment(2L, "pi_456", new BigDecimal("200.00"), PaymentStatus.CAPTURED);
            List<Payment> dbPayments = List.of(dbPayment1, dbPayment2);

            StripePayment stripePayment1 = new StripePayment(
                    "ch_123",
                    "pi_123",
                    new BigDecimal("100.00"),
                    "USD",
                    "succeeded"
            );
            StripePayment stripePayment2 = new StripePayment(
                    "ch_789",
                    "pi_789",
                    new BigDecimal("300.00"),
                    "USD",
                    "succeeded"
            );
            List<StripePayment> stripePayments = List.of(stripePayment1, stripePayment2);

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).hasSize(2);
            assertThat(discrepancies).anyMatch(d -> d.type() == DiscrepancyType.MISSING_IN_STRIPE);
            assertThat(discrepancies).anyMatch(d -> d.type() == DiscrepancyType.MISSING_IN_DATABASE);
        }

        @Test
        @DisplayName("Should ignore payments without payment intent ID")
        void shouldIgnorePaymentsWithoutPaymentIntentId() {
            Payment dbPayment = createPayment(1L, null, new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            List<Payment> dbPayments = List.of(dbPayment);

            List<StripePayment> stripePayments = new ArrayList<>();

            List<Discrepancy> discrepancies = reconciliationService.comparePayments(dbPayments, stripePayments);

            assertThat(discrepancies).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Fetch Tests")
    class DatabaseFetchTests {

        @Test
        @DisplayName("Should fetch payments for specific date")
        void shouldFetchPaymentsForSpecificDate() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            List<Payment> expectedPayments = List.of(
                    createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED)
            );

            when(paymentRepository.findByCreateTimeBetween(startOfDay, endOfDay))
                    .thenReturn(expectedPayments);

            List<Payment> result = reconciliationService.fetchDatabasePayments(date);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return empty list when no payments found")
        void shouldReturnEmptyListWhenNoPaymentsFound() {
            LocalDate date = LocalDate.of(2024, 1, 15);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            when(paymentRepository.findByCreateTimeBetween(startOfDay, endOfDay))
                    .thenReturn(new ArrayList<>());

            List<Payment> result = reconciliationService.fetchDatabasePayments(date);

            assertThat(result).isEmpty();
        }
    }

    private Payment createPayment(Long id, String paymentIntentId, BigDecimal amount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        payment.setStripePaymentIntentId(paymentIntentId);
        payment.setAmount(amount);
        payment.setStatus(status);
        return payment;
    }
}
