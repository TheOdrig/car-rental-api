package com.akif.dashboard.internal.service;

import java.time.Clock;

import com.akif.car.api.CarCatalog;
import com.akif.car.api.CarStatusType;
import com.akif.damage.api.DamageService;
import com.akif.damage.api.RepairCostTotal;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.DailyRevenueDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.MonthlyRevenueDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import com.akif.dashboard.api.dto.RevenueBreakdownDto;
import com.akif.payment.api.CollectedRevenue;
import com.akif.payment.api.MonthlyRevenue;
import com.akif.payment.api.PaymentCategory;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalStatus;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final Clock clock;

    private final RentalService rentalService;
    private final CarCatalog carCatalog;
    private final PaymentService paymentService;
    private final DamageService damageService;

    public DailySummaryDto fetchDailySummary() {
        log.debug("Fetching daily summary data");

        int pendingApprovals = rentalService.countByStatus(RentalStatus.REQUESTED);
        int todaysPickups = rentalService.countTodaysPickups();
        int todaysReturns = rentalService.countTodaysReturns();
        int overdueRentals = rentalService.countOverdueRentals();
        int pendingDamageAssessments = damageService.countPendingAssessments();

        return new DailySummaryDto(
                pendingApprovals,
                todaysPickups,
                todaysReturns,
                overdueRentals,
                pendingDamageAssessments,
                LocalDateTime.now(clock));
    }

    public FleetStatusDto fetchFleetStatus() {
        log.debug("Fetching fleet status data");

        Map<Long, CarStatusType> fleet = carCatalog.findActiveCarStatuses();
        Set<Long> inUse = rentalService.findCarIdsInUse();
        Set<Long> reserved = rentalService.findCarIdsReservedOn(LocalDate.now(clock));

        int rentedCars = 0;
        int reservedCars = 0;
        int availableCars = 0;
        int maintenanceCars = 0;
        int inspectionCars = 0;
        int damagedCars = 0;

        for (Map.Entry<Long, CarStatusType> car : fleet.entrySet()) {
            CarStatusType status = car.getValue();
            if (status == CarStatusType.MAINTENANCE) {
                maintenanceCars++;
            } else if (status == CarStatusType.INSPECTION) {
                inspectionCars++;
            } else if (status == CarStatusType.DAMAGED) {
                damagedCars++;
            } else if (inUse.contains(car.getKey())) {
                rentedCars++;
            } else if (reserved.contains(car.getKey())) {
                reservedCars++;
            } else {
                availableCars++;
            }
        }

        int totalCars = fleet.size();
        BigDecimal occupancyRate = calculateOccupancyRate(rentedCars + reservedCars, totalCars);

        return new FleetStatusDto(
                totalCars,
                availableCars,
                rentedCars,
                reservedCars,
                maintenanceCars,
                inspectionCars,
                damagedCars,
                occupancyRate,
                LocalDateTime.now(clock));
    }

    public MonthlyMetricsDto fetchMonthlyMetrics(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching monthly metrics for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<RevenueBreakdownDto> revenueByCurrency = revenueByCurrency(startDateTime, endDateTime, startDate, endDate);
        int completedRentals = rentalService.countByStatus(RentalStatus.RETURNED);
        int cancelledRentals = rentalService.countByStatus(RentalStatus.CANCELLED);
        BigDecimal averageRentalDurationDays = rentalService.getAverageRentalDurationDays(startDate, endDate);

        return new MonthlyMetricsDto(
                revenueByCurrency,
                completedRentals,
                cancelledRentals,
                averageRentalDurationDays,
                startDate,
                endDate,
                LocalDateTime.now(clock));
    }

    public RevenueAnalyticsDto fetchRevenueAnalytics() {
        log.debug("Fetching revenue analytics");

        List<DailyRevenueDto> dailyRevenue = paymentService.getDailyRevenue(30).stream()
                .map(p -> new DailyRevenueDto(p.date(), p.currency(), p.revenue(), p.paymentCount()))
                .toList();

        List<MonthlyRevenueDto> monthlyRevenue = monthlyRevenueWithGrowth(paymentService.getMonthlyRevenue(12));

        LocalDate endDate = LocalDate.now(clock);
        LocalDate startDate = endDate.minusMonths(12);
        List<RevenueBreakdownDto> breakdown = revenueByCurrency(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), startDate, endDate);

        return new RevenueAnalyticsDto(
                dailyRevenue,
                monthlyRevenue,
                breakdown,
                LocalDateTime.now(clock));
    }

    private List<MonthlyRevenueDto> monthlyRevenueWithGrowth(List<MonthlyRevenue> months) {
        Map<CurrencyType, BigDecimal> previous = new EnumMap<>(CurrencyType.class);
        Map<CurrencyType, YearMonth> previousMonth = new EnumMap<>(CurrencyType.class);
        List<MonthlyRevenueDto> result = new ArrayList<>();
        for (MonthlyRevenue month : months) {
            BigDecimal growth = BigDecimal.ZERO;
            BigDecimal before = previous.get(month.currency());
            if (before != null && before.signum() > 0
                    && month.month().minusMonths(1).equals(previousMonth.get(month.currency()))) {
                growth = month.revenue().subtract(before)
                        .divide(before, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            previous.put(month.currency(), month.revenue());
            previousMonth.put(month.currency(), month.month());
            result.add(new MonthlyRevenueDto(
                    month.month(), month.currency(), month.revenue(), month.paymentCount(), growth));
        }
        return result;
    }

    public Page<PendingItemDto> fetchPendingApprovals(Pageable pageable) {
        Page<RentalResponse> rentals = rentalService.findPendingApprovals(pageable);
        return rentals.map(this::toPendingItemDto);
    }

    public Page<PendingItemDto> fetchTodaysPickups(Pageable pageable) {
        Page<RentalResponse> rentals = rentalService.findTodaysPickups(pageable);
        return rentals.map(this::toPendingItemDto);
    }

    public Page<PendingItemDto> fetchTodaysReturns(Pageable pageable) {
        Page<RentalResponse> rentals = rentalService.findTodaysReturns(pageable);
        return rentals.map(this::toPendingItemDto);
    }

    public Page<PendingItemDto> fetchOverdueRentals(Pageable pageable) {
        Page<RentalResponse> rentals = rentalService.findOverdueRentals(pageable);
        return rentals.map(this::toPendingItemDto);
    }

    private BigDecimal calculateOccupancyRate(int rentedCars, int totalCars) {
        if (totalCars == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(rentedCars)
                .divide(BigDecimal.valueOf(totalCars), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private List<RevenueBreakdownDto> revenueByCurrency(
            LocalDateTime from, LocalDateTime to, LocalDate startDate, LocalDate endDate) {
        Map<CurrencyType, Map<PaymentCategory, BigDecimal>> collected = new EnumMap<>(CurrencyType.class);
        for (CollectedRevenue revenue : paymentService.sumCollectedBetween(from, to)) {
            collected.computeIfAbsent(revenue.currency(), key -> new EnumMap<>(PaymentCategory.class))
                    .merge(revenue.category(), revenue.amount(), BigDecimal::add);
        }
        Map<CurrencyType, BigDecimal> repairCosts = new EnumMap<>(CurrencyType.class);
        for (RepairCostTotal total : damageService.sumRepairCostsByCurrency(startDate, endDate)) {
            repairCosts.merge(total.currency(), total.amount(), BigDecimal::add);
        }
        Set<CurrencyType> currencies = EnumSet.noneOf(CurrencyType.class);
        currencies.addAll(collected.keySet());
        currencies.addAll(repairCosts.keySet());

        return currencies.stream()
                .map(currency -> breakdown(
                        currency,
                        collected.getOrDefault(currency, Map.of()),
                        repairCosts.getOrDefault(currency, BigDecimal.ZERO)))
                .toList();
    }

    private RevenueBreakdownDto breakdown(
            CurrencyType currency, Map<PaymentCategory, BigDecimal> byCategory, BigDecimal damageRepairCosts) {
        BigDecimal rentalRevenue = byCategory.getOrDefault(PaymentCategory.RENTAL, BigDecimal.ZERO);
        BigDecimal penaltyRevenue = byCategory.getOrDefault(PaymentCategory.PENALTY, BigDecimal.ZERO);
        BigDecimal damageRecovered = byCategory.getOrDefault(PaymentCategory.DAMAGE, BigDecimal.ZERO);

        BigDecimal netDamageImpact = damageRecovered.subtract(damageRepairCosts);
        BigDecimal totalRevenue = rentalRevenue.add(penaltyRevenue).add(damageRecovered);
        BigDecimal netRevenue = rentalRevenue.add(penaltyRevenue).add(netDamageImpact);

        return new RevenueBreakdownDto(
                currency,
                rentalRevenue,
                penaltyRevenue,
                damageRecovered,
                damageRepairCosts,
                netDamageImpact,
                totalRevenue,
                netRevenue,
                percentage(rentalRevenue, totalRevenue),
                percentage(penaltyRevenue, totalRevenue),
                percentage(damageRecovered, totalRevenue));
    }

    private static BigDecimal percentage(BigDecimal part, BigDecimal total) {
        if (total.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return part.divide(total, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
    }

    private PendingItemDto toPendingItemDto(RentalResponse rental) {
        Long lateHours = calculateLateHours(rental.endDate());

        return new PendingItemDto(
                rental.id(),
                rental.userSummary() != null ? rental.userSummary().id() : null,
                rental.userSummary() != null ? rental.userSummary().username() : null,
                rental.userSummary() != null ? rental.userSummary().email() : null,
                rental.carSummary() != null ? rental.carSummary().id() : null,
                rental.carSummary() != null ? rental.carSummary().brand() : null,
                rental.carSummary() != null ? rental.carSummary().model() : null,
                rental.carSummary() != null ? rental.carSummary().licensePlate() : null,
                rental.carSummary() != null ? rental.carSummary().thumbnailUrl() : null,
                rental.startDate(),
                rental.endDate(),
                rental.totalPrice(),
                rental.status() != null ? rental.status().name() : null,
                lateHours,
                rental.createTime());
    }

    private Long calculateLateHours(LocalDate endDate) {
        if (endDate == null || !endDate.isBefore(LocalDate.now(clock))) {
            return null;
        }
        return java.time.temporal.ChronoUnit.HOURS.between(
                endDate.atStartOfDay(),
                LocalDateTime.now(clock));
    }
}