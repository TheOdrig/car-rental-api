package com.akif.payment.internal.service;

import com.akif.shared.time.UtcDates;
import java.time.ZoneOffset;
import java.time.Duration;
import java.time.Clock;
import com.akif.payment.api.CollectedRevenue;
import com.akif.payment.api.DailyRevenue;
import com.akif.payment.api.MonthlyRevenue;
import com.akif.payment.api.NetPaid;
import com.akif.payment.api.PaymentCategory;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.domain.Payment;
import com.akif.payment.domain.enums.PaymentKind;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.stream.Collectors;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
class PaymentRevenueService {

    private final Clock clock;
    private final PaymentRepository paymentRepository;

    public List<CollectedRevenue> sumCollectedBetween(Instant start, Instant end) {
        return paymentRepository.sumCollectedBetween(start, end).stream()
                .map(row -> new CollectedRevenue(
                        PaymentCategory.valueOf(((PaymentKind) row[0]).name()),
                        (CurrencyType) row[1],
                        (BigDecimal) row[2],
                        (Long) row[3]))
                .toList();
    }

    public List<NetPaid> sumNetPaidForRentals(java.util.Collection<Long> rentalIds) {
        if (rentalIds == null || rentalIds.isEmpty()) {
            return List.of();
        }
        return paymentRepository.sumNetPaidByRentalIds(rentalIds).stream()
                .map(row -> new NetPaid((CurrencyType) row[0], (Long) row[1], (BigDecimal) row[2]))
                .toList();
    }

    public List<DailyRevenue> getDailyRevenue(int days) {
        Instant startDate = Instant.now(clock).minus(Duration.ofDays(days));
        Map<RevenueKey<LocalDate>, List<Payment>> grouped = paymentRepository.findCapturedSince(startDate).stream()
                .collect(Collectors.groupingBy(p -> new RevenueKey<>(UtcDates.toDate(p.getCapturedAt()), p.getCurrency())));

        return grouped.entrySet().stream()
                .map(entry -> new DailyRevenue(
                        entry.getKey().period(),
                        entry.getKey().currency(),
                        netCollected(entry.getValue()),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(DailyRevenue::date).thenComparing(DailyRevenue::currency))
                .toList();
    }

    public List<MonthlyRevenue> getMonthlyRevenue(int months) {
        Instant startDate = Instant.now(clock).atZone(ZoneOffset.UTC).minusMonths(months).toInstant();
        Map<RevenueKey<YearMonth>, List<Payment>> grouped = paymentRepository.findCapturedSince(startDate).stream()
                .collect(Collectors.groupingBy(p -> new RevenueKey<>(YearMonth.from(UtcDates.toDate(p.getCapturedAt())), p.getCurrency())));

        return grouped.entrySet().stream()
                .map(entry -> new MonthlyRevenue(
                        entry.getKey().period(),
                        entry.getKey().currency(),
                        netCollected(entry.getValue()),
                        entry.getValue().size()))
                .sorted(Comparator.comparing(MonthlyRevenue::month).thenComparing(MonthlyRevenue::currency))
                .toList();
    }

    private static BigDecimal netCollected(List<Payment> payments) {
        return payments.stream()
                .map(p -> p.getAmount().subtract(p.refundedTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record RevenueKey<T>(T period, CurrencyType currency) {
    }

    public int countFailedPayments() {
        return paymentRepository.countByStatusAndIsDeletedFalse(PaymentStatus.FAILED);
    }
}
