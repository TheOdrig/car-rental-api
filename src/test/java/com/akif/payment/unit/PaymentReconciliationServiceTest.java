package com.akif.payment.unit;

import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.DiscrepancyType;
import com.akif.payment.internal.dto.Discrepancy;
import com.akif.payment.internal.dto.StripePayment;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.reconciliation.PaymentReconciliationService;
import com.stripe.model.Charge;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentReconciliationService Tests")
class PaymentReconciliationServiceTest {

    private static final LocalDate DAY = LocalDate.of(2024, 1, 15);

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentReconciliationService reconciliationService;

    private Charge charge(long amount, String currency, String status, boolean captured, boolean refunded) {
        Charge charge = new Charge();
        charge.setId("ch_1");
        charge.setPaymentIntent("pi_1");
        charge.setAmount(amount);
        charge.setCurrency(currency);
        charge.setStatus(status);
        charge.setCaptured(captured);
        charge.setRefunded(refunded);
        charge.setCreated(DAY.atStartOfDay().toEpochSecond(java.time.ZoneOffset.UTC) + 3600);
        return charge;
    }

    private StripePayment stripe(String intentId, String amount, String status, LocalDate createdOn) {
        return new StripePayment("ch_" + intentId, intentId, new BigDecimal(amount), "USD", status, createdOn);
    }

    private List<Discrepancy> compare(List<Payment> db, List<StripePayment> stripe, Set<String> known) {
        return reconciliationService.comparePayments(db, stripe, known, DAY);
    }

    @Nested
    @DisplayName("Stripe amount conversion tests")
    class StripeAmountConversionTests {

        @Test
        @DisplayName("Should keep zero-decimal JPY amounts as they are")
        void shouldNotDivideJpyAmounts() {
            StripePayment payment = reconciliationService.toStripePayment(charge(15000L, "jpy", "succeeded", true, false));

            assertThat(payment.amount()).isEqualByComparingTo("15000");
            assertThat(payment.currency()).isEqualTo("JPY");
        }

        @Test
        @DisplayName("Should divide two-decimal amounts by 100")
        void shouldDivideTwoDecimalAmounts() {
            StripePayment payment = reconciliationService.toStripePayment(charge(1050L, "usd", "succeeded", true, false));

            assertThat(payment.amount()).isEqualByComparingTo("10.50");
            assertThat(payment.currency()).isEqualTo("USD");
            assertThat(payment.createdOn()).isEqualTo(DAY);
        }

        @Test
        @DisplayName("Should not report a mismatch for a JPY payment that matches Stripe")
        void shouldNotReportJpyMismatch() {
            Payment dbPayment = createPayment(1L, "pi_1", new BigDecimal("15000"), PaymentStatus.CAPTURED);
            StripePayment stripePayment = reconciliationService.toStripePayment(charge(15000L, "jpy", "succeeded", true, false));

            List<Discrepancy> discrepancies = compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_1"));

            assertThat(discrepancies).noneMatch(d -> d.type() == DiscrepancyType.AMOUNT_MISMATCH);
        }
    }

    @Nested
    @DisplayName("Charge status normalisation")
    class ChargeStatusNormalisation {

        @Test
        @DisplayName("An uncaptured succeeded charge is an authorization, not a capture")
        void uncapturedSucceededIsAuthorized() {
            assertThat(reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", false, false)).status())
                    .isEqualTo("AUTHORIZED");
        }

        @Test
        @DisplayName("A captured succeeded charge is captured")
        void capturedSucceededIsCaptured() {
            assertThat(reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", true, false)).status())
                    .isEqualTo("CAPTURED");
        }

        @Test
        @DisplayName("A fully refunded captured charge is refunded")
        void refundedCapturedIsRefunded() {
            assertThat(reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", true, true)).status())
                    .isEqualTo("REFUNDED");
        }

        @Test
        @DisplayName("A released authorization is cancelled")
        void releasedAuthorizationIsCancelled() {
            assertThat(reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", false, true)).status())
                    .isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("A failed charge is failed")
        void failedIsFailed() {
            assertThat(reconciliationService.toStripePayment(charge(1000L, "usd", "failed", false, false)).status())
                    .isEqualTo("FAILED");
        }

        @Test
        @DisplayName("An authorized database payment matches an uncaptured charge")
        void authorizedPaymentMatchesAnUncapturedCharge() {
            Payment dbPayment = createPayment(1L, "pi_1", new BigDecimal("10.00"), PaymentStatus.AUTHORIZED);
            StripePayment stripePayment = reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", false, false));

            assertThat(compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_1"))).isEmpty();
        }

        @Test
        @DisplayName("A refunded database payment matches a fully refunded charge")
        void refundedPaymentMatchesARefundedCharge() {
            Payment dbPayment = createPayment(1L, "pi_1", new BigDecimal("10.00"), PaymentStatus.REFUNDED);
            StripePayment stripePayment = reconciliationService.toStripePayment(charge(1000L, "usd", "succeeded", true, true));

            assertThat(compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_1"))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Discrepancy Detection Tests")
    class DiscrepancyDetectionTests {

        @Test
        @DisplayName("Should detect a captured payment missing in Stripe")
        void shouldDetectPaymentMissingInStripe() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);

            List<Discrepancy> discrepancies = compare(List.of(dbPayment), new ArrayList<>(), Set.of());

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.MISSING_IN_STRIPE);
            assertThat(discrepancies.get(0).paymentId()).isEqualTo("1");
            assertThat(discrepancies.get(0).stripePaymentIntentId()).isEqualTo("pi_123");
        }

        @Test
        @DisplayName("Should not call a payment without a charge missing when no money moved")
        void shouldIgnoreUnchargedPayments() {
            Payment pending = createPayment(1L, "pi_1", new BigDecimal("100.00"), PaymentStatus.PENDING);
            Payment cancelled = createPayment(2L, "pi_2", new BigDecimal("100.00"), PaymentStatus.CANCELLED);

            assertThat(compare(List.of(pending, cancelled), new ArrayList<>(), Set.of())).isEmpty();
        }

        @Test
        @DisplayName("Should find the charge of a payment that was created days before it was authorized")
        void shouldMatchAnAuthorizationMadeOnAnEarlierDay() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            StripePayment charge = stripe("pi_123", "100.00", "CAPTURED", DAY.minusDays(4));

            assertThat(compare(List.of(dbPayment), List.of(charge), Set.of("pi_123"))).isEmpty();
        }

        @Test
        @DisplayName("Should detect a charge created that day that is missing in the database")
        void shouldDetectPaymentMissingInDatabase() {
            StripePayment stripePayment = stripe("pi_456", "100.00", "CAPTURED", DAY);

            List<Discrepancy> discrepancies = compare(new ArrayList<>(), List.of(stripePayment), Set.of());

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.MISSING_IN_DATABASE);
            assertThat(discrepancies.get(0).stripePaymentIntentId()).isEqualTo("pi_456");
        }

        @Test
        @DisplayName("Should not report a charge of an earlier day as missing, it belongs to that day's run")
        void shouldNotReportEarlierChargesMissing() {
            StripePayment stripePayment = stripe("pi_456", "100.00", "CAPTURED", DAY.minusDays(3));

            assertThat(compare(new ArrayList<>(), List.of(stripePayment), Set.of())).isEmpty();
        }

        @Test
        @DisplayName("Should not report a charge that exists in the database under another day")
        void shouldNotReportKnownCharges() {
            StripePayment stripePayment = stripe("pi_456", "100.00", "CAPTURED", DAY);

            assertThat(compare(new ArrayList<>(), List.of(stripePayment), Set.of("pi_456"))).isEmpty();
        }

        @Test
        @DisplayName("Should detect amount mismatch")
        void shouldDetectAmountMismatch() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            StripePayment stripePayment = stripe("pi_123", "150.00", "CAPTURED", DAY);

            List<Discrepancy> discrepancies = compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_123"));

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.AMOUNT_MISMATCH);
            assertThat(discrepancies.get(0).databaseAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(discrepancies.get(0).stripeAmount()).isEqualByComparingTo(new BigDecimal("150.00"));
        }

        @Test
        @DisplayName("Should detect status mismatch")
        void shouldDetectStatusMismatch() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.PENDING);
            StripePayment stripePayment = stripe("pi_123", "100.00", "CAPTURED", DAY);

            List<Discrepancy> discrepancies = compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_123"));

            assertThat(discrepancies).hasSize(1);
            assertThat(discrepancies.get(0).type()).isEqualTo(DiscrepancyType.STATUS_MISMATCH);
            assertThat(discrepancies.get(0).databaseStatus()).isEqualTo("PENDING");
            assertThat(discrepancies.get(0).stripeStatus()).isEqualTo("CAPTURED");
        }

        @Test
        @DisplayName("Should not report discrepancy for matching payments")
        void shouldNotReportDiscrepancyForMatchingPayments() {
            Payment dbPayment = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            StripePayment stripePayment = stripe("pi_123", "100.00", "CAPTURED", DAY);

            assertThat(compare(List.of(dbPayment), List.of(stripePayment), Set.of("pi_123"))).isEmpty();
        }

        @Test
        @DisplayName("Should handle multiple discrepancies")
        void shouldHandleMultipleDiscrepancies() {
            Payment dbPayment1 = createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED);
            Payment dbPayment2 = createPayment(2L, "pi_456", new BigDecimal("200.00"), PaymentStatus.CAPTURED);
            List<StripePayment> stripePayments = List.of(
                    stripe("pi_123", "100.00", "CAPTURED", DAY),
                    stripe("pi_789", "300.00", "CAPTURED", DAY));

            List<Discrepancy> discrepancies = compare(List.of(dbPayment1, dbPayment2), stripePayments, Set.of("pi_123"));

            assertThat(discrepancies).hasSize(2);
            assertThat(discrepancies).anyMatch(d -> d.type() == DiscrepancyType.MISSING_IN_STRIPE);
            assertThat(discrepancies).anyMatch(d -> d.type() == DiscrepancyType.MISSING_IN_DATABASE);
        }

        @Test
        @DisplayName("Should ignore payments without payment intent ID")
        void shouldIgnorePaymentsWithoutPaymentIntentId() {
            Payment dbPayment = createPayment(1L, null, new BigDecimal("100.00"), PaymentStatus.CAPTURED);

            assertThat(compare(List.of(dbPayment), new ArrayList<>(), Set.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Database Fetch Tests")
    class DatabaseFetchTests {

        @Test
        @DisplayName("Should fetch the payments touched on a specific date")
        void shouldFetchPaymentsForSpecificDate() {
            LocalDateTime startOfDay = DAY.atStartOfDay();
            LocalDateTime endOfDay = DAY.plusDays(1).atStartOfDay();
            List<Payment> expectedPayments = List.of(
                    createPayment(1L, "pi_123", new BigDecimal("100.00"), PaymentStatus.CAPTURED));
            when(paymentRepository.findWithIntentUpdatedBetween(startOfDay, endOfDay)).thenReturn(expectedPayments);

            List<Payment> result = reconciliationService.fetchDatabasePayments(DAY);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return empty list when no payments found")
        void shouldReturnEmptyListWhenNoPaymentsFound() {
            when(paymentRepository.findWithIntentUpdatedBetween(DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay()))
                    .thenReturn(new ArrayList<>());

            assertThat(reconciliationService.fetchDatabasePayments(DAY)).isEmpty();
        }
    }

    private Payment createPayment(Long id, String paymentIntentId, BigDecimal amount, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setId(id);
        ReflectionTestUtils.setField(payment, "stripePaymentIntentId", paymentIntentId);
        ReflectionTestUtils.setField(payment, "amount", amount);
        ReflectionTestUtils.setField(payment, "status", status);
        return payment;
    }

    @Nested
    @DisplayName("Individual lookups for intents outside the listed window")
    class IndividualLookupTests {

        private Payment oldCapturedPayment() {
            Payment payment = new Payment();
            ReflectionTestUtils.setField(payment, "id", 9L);
            ReflectionTestUtils.setField(payment, "stripePaymentIntentId", "pi_old");
            ReflectionTestUtils.setField(payment, "status", PaymentStatus.REFUNDED);
            ReflectionTestUtils.setField(payment, "amount", new BigDecimal("100.00"));
            return payment;
        }

        @Test
        @DisplayName("a refund on a charge older than the listing window is looked up, not reported missing")
        void oldChargeIsLookedUpIndividually() {
            PaymentReconciliationService spy = org.mockito.Mockito.spy(reconciliationService);
            org.mockito.Mockito.doReturn(stripe("pi_old", "100.00", "REFUNDED", null)).when(spy).fetchIntentCharge("pi_old");
            List<Payment> db = List.of(oldCapturedPayment());

            List<StripePayment> looked = spy.withIndividualLookups(db, List.of());
            List<Discrepancy> found = spy.comparePayments(db, looked, Set.of(), DAY);

            assertThat(looked).hasSize(1);
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("an intent Stripe really does not know is still reported missing")
        void trulyMissingIntentIsStillReported() {
            PaymentReconciliationService spy = org.mockito.Mockito.spy(reconciliationService);
            org.mockito.Mockito.doReturn(null).when(spy).fetchIntentCharge("pi_old");
            List<Payment> db = List.of(oldCapturedPayment());

            List<StripePayment> looked = spy.withIndividualLookups(db, List.of());
            List<Discrepancy> found = spy.comparePayments(db, looked, Set.of(), DAY);

            assertThat(found).extracting(Discrepancy::type).containsExactly(DiscrepancyType.MISSING_IN_STRIPE);
        }

        @Test
        @DisplayName("intents already in the listing are not looked up again")
        void listedIntentsAreNotLookedUp() {
            PaymentReconciliationService spy = org.mockito.Mockito.spy(reconciliationService);
            List<Payment> db = List.of(oldCapturedPayment());

            spy.withIndividualLookups(db, List.of(stripe("pi_old", "100.00", "REFUNDED", DAY)));

            org.mockito.Mockito.verify(spy, org.mockito.Mockito.never()).fetchIntentCharge(org.mockito.ArgumentMatchers.anyString());
        }
    }
}
