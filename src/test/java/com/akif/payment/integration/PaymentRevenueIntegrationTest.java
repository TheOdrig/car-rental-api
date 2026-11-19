package com.akif.payment.integration;

import com.akif.shared.time.UtcDates;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.Duration;
import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.CollectedRevenue;
import com.akif.payment.api.DailyRevenue;
import com.akif.payment.api.MonthlyRevenue;
import com.akif.payment.api.PaymentCategory;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.PaymentStateService;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.shared.enums.CurrencyType;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Collected revenue by kind, currency and capture time")
class PaymentRevenueIntegrationTest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentStateService paymentStateService;

    private Rental rental;

    @BeforeEach
    void seedRental() {
        User user = userRepository.save(TestDataBuilder.createTestUser());
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        rental = rentalRepository.save(TestDataBuilder.createRental(
                car, user, today.minusDays(10), today.minusDays(5), RentalStatus.RETURNED));
    }

    private Rental otherRental() {
        User user = userRepository.findAll().get(0);
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        LocalDate today = LocalDate.now(clock);
        return rentalRepository.save(TestDataBuilder.createRental(
                car, user, today.minusDays(10), today.minusDays(5), RentalStatus.RETURNED));
    }

    private Payment payment(PaymentKind kind, CurrencyType currency, String amount, String refunded,
                            PaymentStatus status, Instant capturedAt) {
        Long rentalId = kind == PaymentKind.RENTAL ? otherRental().getId() : rental.getId();
        Payment payment = Payment.builder()
                .rentalId(rentalId)
                .kind(kind)
                .userEmail(rental.getUserEmail())
                .carLicensePlate(rental.getCarLicensePlate())
                .amount(new BigDecimal(amount))
                .currency(currency)
                .status(status)
                .paymentMethod(kind.name())
                .stripePaymentIntentId("pi_" + kind + "_" + currency + "_" + amount + "_" + System.nanoTime())
                .refundedAmount(refunded == null ? null : new BigDecimal(refunded))
                .capturedAt(capturedAt)
                .isDeleted(false)
                .build();
        return paymentRepository.save(payment);
    }

    private CollectedRevenue find(List<CollectedRevenue> rows, PaymentCategory category, CurrencyType currency) {
        return rows.stream()
                .filter(row -> row.category() == category && row.currency() == currency)
                .findFirst()
                .orElseThrow();
    }

    @Test
    @DisplayName("revenue is grouped by kind and currency, net of refunds, and counts only captured payments")
    void groupsByKindAndCurrencyNetOfRefunds() {
        Instant at = LocalDateTime.of(2031, 3, 10, 12, 0).toInstant(ZoneOffset.UTC);
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "1000.00", null, PaymentStatus.CAPTURED, at);
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "500.00", "200.00", PaymentStatus.CAPTURED, at);
        payment(PaymentKind.PENALTY, CurrencyType.TRY, "150.00", null, PaymentStatus.CAPTURED, at);
        payment(PaymentKind.DAMAGE, CurrencyType.TRY, "300.00", null, PaymentStatus.CAPTURED, at);
        payment(PaymentKind.RENTAL, CurrencyType.USD, "100.00", null, PaymentStatus.CAPTURED, at);
        payment(PaymentKind.RENTAL, CurrencyType.JPY, "10000.00", null, PaymentStatus.CAPTURED, at);
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "700.00", "700.00", PaymentStatus.REFUNDED, at);
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "800.00", null, PaymentStatus.AUTHORIZED, null);

        List<CollectedRevenue> rows = paymentService.sumCollectedBetween(
                LocalDateTime.of(2031, 3, 1, 0, 0).toInstant(ZoneOffset.UTC), LocalDateTime.of(2031, 4, 1, 0, 0).toInstant(ZoneOffset.UTC));

        assertThat(rows).hasSize(5);
        assertThat(find(rows, PaymentCategory.RENTAL, CurrencyType.TRY).amount()).isEqualByComparingTo("1300.00");
        assertThat(find(rows, PaymentCategory.RENTAL, CurrencyType.TRY).paymentCount()).isEqualTo(2);
        assertThat(find(rows, PaymentCategory.PENALTY, CurrencyType.TRY).amount()).isEqualByComparingTo("150.00");
        assertThat(find(rows, PaymentCategory.DAMAGE, CurrencyType.TRY).amount()).isEqualByComparingTo("300.00");
        assertThat(find(rows, PaymentCategory.RENTAL, CurrencyType.USD).amount()).isEqualByComparingTo("100.00");
        assertThat(find(rows, PaymentCategory.RENTAL, CurrencyType.JPY).amount()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("a payment created in March and captured in April belongs to April")
    void attributedToTheCaptureTime() {
        Payment created = payment(PaymentKind.RENTAL, CurrencyType.TRY, "900.00", null, PaymentStatus.CAPTURED,
                LocalDateTime.of(2031, 4, 2, 9, 0).toInstant(ZoneOffset.UTC));
        jdbcTemplate.update("UPDATE car_rental.payments SET create_time = ? WHERE id = ?",
                java.sql.Timestamp.from(LocalDateTime.of(2031, 3, 1, 9, 0).toInstant(ZoneOffset.UTC)), created.getId());

        List<CollectedRevenue> march = paymentService.sumCollectedBetween(
                LocalDateTime.of(2031, 3, 1, 0, 0).toInstant(ZoneOffset.UTC), LocalDateTime.of(2031, 4, 1, 0, 0).toInstant(ZoneOffset.UTC));
        List<CollectedRevenue> april = paymentService.sumCollectedBetween(
                LocalDateTime.of(2031, 4, 1, 0, 0).toInstant(ZoneOffset.UTC), LocalDateTime.of(2031, 5, 1, 0, 0).toInstant(ZoneOffset.UTC));

        assertThat(march).isEmpty();
        assertThat(april).hasSize(1);
        assertThat(april.get(0).amount()).isEqualByComparingTo("900.00");
    }

    @Test
    @DisplayName("daily and monthly charts are grouped per currency from the capture time")
    void chartsAreGroupedPerCurrency() {
        Instant now = Instant.now(clock);
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "100.00", null, PaymentStatus.CAPTURED, now.minus(Duration.ofDays(1)));
        payment(PaymentKind.PENALTY, CurrencyType.TRY, "50.00", "20.00", PaymentStatus.CAPTURED, now.minus(Duration.ofDays(1)));
        payment(PaymentKind.RENTAL, CurrencyType.USD, "70.00", null, PaymentStatus.CAPTURED, now.minus(Duration.ofDays(1)));
        payment(PaymentKind.RENTAL, CurrencyType.TRY, "10.00", null, PaymentStatus.CAPTURED, now.minus(Duration.ofDays(45)));

        List<DailyRevenue> daily = paymentService.getDailyRevenue(30);
        List<MonthlyRevenue> monthly = paymentService.getMonthlyRevenue(12);

        assertThat(daily).hasSize(2);
        DailyRevenue lira = daily.stream().filter(d -> d.currency() == CurrencyType.TRY).findFirst().orElseThrow();
        assertThat(lira.date()).isEqualTo(UtcDates.toDate(now.minus(Duration.ofDays(1))));
        assertThat(lira.revenue()).isEqualByComparingTo("130.00");
        assertThat(lira.paymentCount()).isEqualTo(2);
        assertThat(daily.stream().filter(d -> d.currency() == CurrencyType.USD).findFirst().orElseThrow().revenue())
                .isEqualByComparingTo("70.00");
        assertThat(monthly).extracting(MonthlyRevenue::currency).contains(CurrencyType.TRY, CurrencyType.USD);
        assertThat(monthly.stream().mapToInt(MonthlyRevenue::paymentCount).sum()).isEqualTo(4);
        assertThat(monthly).allSatisfy(m -> assertThat(m.month().isAfter(YearMonth.from(UtcDates.toDate(now)))).isFalse());
    }

    @Test
    @DisplayName("what a user paid is the net of refunds across rental, penalty and damage payments, per currency")
    void netPaidIncludesEveryKindAfterRefunds() {
        Instant at = LocalDateTime.of(2031, 3, 10, 12, 0).toInstant(ZoneOffset.UTC);
        Rental second = otherRental();
        savePaymentFor(rental.getId(), PaymentKind.RENTAL, CurrencyType.TRY, "1000.00", "400.00", PaymentStatus.CAPTURED, at);
        savePaymentFor(rental.getId(), PaymentKind.PENALTY, CurrencyType.TRY, "150.00", null, PaymentStatus.CAPTURED, at);
        savePaymentFor(rental.getId(), PaymentKind.DAMAGE, CurrencyType.TRY, "300.00", "50.00", PaymentStatus.CAPTURED, at);
        savePaymentFor(second.getId(), PaymentKind.RENTAL, CurrencyType.TRY, "200.00", null, PaymentStatus.CAPTURED, at);
        savePaymentFor(second.getId(), PaymentKind.PENALTY, CurrencyType.USD, "20.00", null, PaymentStatus.CAPTURED, at);
        savePaymentFor(second.getId(), PaymentKind.DAMAGE, CurrencyType.TRY, "90.00", null, PaymentStatus.FAILED, null);

        var paid = paymentService.sumNetPaidForRentals(List.of(rental.getId(), second.getId()));

        assertThat(paid).hasSize(2);
        var lira = paid.stream().filter(p -> p.currency() == CurrencyType.TRY).findFirst().orElseThrow();
        assertThat(lira.amount()).isEqualByComparingTo("1200.00");
        assertThat(lira.rentalCount()).isEqualTo(2);
        var dollars = paid.stream().filter(p -> p.currency() == CurrencyType.USD).findFirst().orElseThrow();
        assertThat(dollars.amount()).isEqualByComparingTo("20.00");
        assertThat(paymentService.sumNetPaidForRentals(List.of())).isEmpty();
    }

    private Payment savePaymentFor(Long rentalId, PaymentKind kind, CurrencyType currency, String amount, String refunded,
                                   PaymentStatus status, Instant capturedAt) {
        return paymentRepository.save(Payment.builder()
                .rentalId(rentalId)
                .kind(kind)
                .userEmail("owner@example.com")
                .carLicensePlate("34TEST1")
                .amount(new BigDecimal(amount))
                .currency(currency)
                .status(status)
                .paymentMethod(kind.name())
                .stripePaymentIntentId("pi_net_" + rentalId + "_" + kind + "_" + currency + "_" + System.nanoTime())
                .refundedAmount(refunded == null ? null : new BigDecimal(refunded))
                .capturedAt(capturedAt)
                .isDeleted(false)
                .build());
    }

    @Test
    @DisplayName("capturing a payment stamps the capture time once")
    void captureStampsTheTime() {
        Payment pending = payment(PaymentKind.RENTAL, CurrencyType.TRY, "100.00", null, PaymentStatus.AUTHORIZED, null);

        paymentStateService.markCaptured(paymentRepository.findById(pending.getId()).orElseThrow(), "pi_captured");
        Instant first = paymentRepository.findById(pending.getId()).orElseThrow().getCapturedAt();
        clock.advance(java.time.Duration.ofHours(3));
        paymentStateService.markCaptured(paymentRepository.findById(pending.getId()).orElseThrow(), "pi_captured");

        assertThat(first).isNotNull();
        assertThat(paymentRepository.findById(pending.getId()).orElseThrow().getCapturedAt()).isEqualTo(first);
    }
}
