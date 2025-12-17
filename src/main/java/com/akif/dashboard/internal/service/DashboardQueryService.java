package com.akif.dashboard.internal.service;

import com.akif.car.api.CarService;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.damage.api.DamageService;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.DailyRevenueDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.MonthlyRevenueDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import com.akif.dashboard.api.dto.RevenueBreakdownDto;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.domain.enums.RentalStatus;
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
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardQueryService {

    private final RentalService rentalService;
    private final CarService carService;
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
            LocalDateTime.now()
        );
    }

    public FleetStatusDto fetchFleetStatus() {
        log.debug("Fetching fleet status data");
        
        int totalCars = carService.countTotalActiveCars();
        int availableCars = carService.countByStatus(CarStatusType.AVAILABLE);
        int rentedCars = carService.countByStatus(CarStatusType.RESERVED);
        int maintenanceCars = carService.countByStatus(CarStatusType.MAINTENANCE);
        int damagedCars = carService.countByStatus(CarStatusType.DAMAGED);
        
        BigDecimal occupancyRate = calculateOccupancyRate(rentedCars, totalCars);
        
        return new FleetStatusDto(
            totalCars,
            availableCars,
            rentedCars,
            maintenanceCars,
            damagedCars,
            occupancyRate,
            LocalDateTime.now()
        );
    }

    public MonthlyMetricsDto fetchMonthlyMetrics(LocalDate startDate, LocalDate endDate) {
        log.debug("Fetching monthly metrics for period: {} to {}", startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        BigDecimal totalRevenue = paymentService.sumCapturedPaymentsBetween(startDateTime, endDateTime);
        int completedRentals = rentalService.countByStatus(RentalStatus.RETURNED);
        int cancelledRentals = rentalService.countByStatus(RentalStatus.CANCELLED);
        
        BigDecimal penaltyRevenue = rentalService.sumCollectedPenaltyRevenue(startDate, endDate);
        BigDecimal damageCharges = damageService.sumDamageCharges(startDate, endDate);
        BigDecimal averageRentalDurationDays = rentalService.getAverageRentalDurationDays(startDate, endDate);
        
        return new MonthlyMetricsDto(
            totalRevenue,
            completedRentals,
            cancelledRentals,
            penaltyRevenue,
            damageCharges,
            averageRentalDurationDays,
            startDate,
            endDate,
            LocalDateTime.now()
        );
    }

    public RevenueAnalyticsDto fetchRevenueAnalytics() {
        log.debug("Fetching revenue analytics");
        
        List<PaymentService.DailyRevenueProjection> dailyProjections = paymentService.getDailyRevenue(30);
        List<PaymentService.MonthlyRevenueProjection> monthlyProjections = paymentService.getMonthlyRevenue(12);
        
        List<DailyRevenueDto> dailyRevenue = dailyProjections.stream()
            .map(p -> new DailyRevenueDto(p.getDate(), p.getRevenue(), p.getRentalCount()))
            .toList();
        
        List<MonthlyRevenueDto> monthlyRevenue = monthlyProjections.stream()
            .map(p -> new MonthlyRevenueDto(p.getMonth(), p.getRevenue(), p.getRentalCount(), BigDecimal.ZERO))
            .toList();
        
        RevenueBreakdownDto breakdown = calculateRevenueBreakdown();
        
        return new RevenueAnalyticsDto(
            dailyRevenue,
            monthlyRevenue,
            breakdown,
            LocalDateTime.now()
        );
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

    private RevenueBreakdownDto calculateRevenueBreakdown() {
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDateTime.now();
        LocalDate startDate = LocalDate.now().withDayOfMonth(1);
        LocalDate endDate = LocalDate.now();
        
        BigDecimal rentalRevenue = paymentService.sumCapturedPaymentsBetween(startOfMonth, endOfMonth);
        BigDecimal penaltyRevenue = rentalService.sumCollectedPenaltyRevenue(startDate, endDate);
        BigDecimal damageCharges = damageService.sumDamageCharges(startDate, endDate);
        
        BigDecimal totalRevenue = rentalRevenue.add(penaltyRevenue).add(damageCharges);
        
        BigDecimal rentalPercentage = BigDecimal.ZERO;
        BigDecimal penaltyPercentage = BigDecimal.ZERO;
        BigDecimal damagePercentage = BigDecimal.ZERO;
        
        if (totalRevenue.compareTo(BigDecimal.ZERO) > 0) {
            rentalPercentage = rentalRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            penaltyPercentage = penaltyRevenue.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
            damagePercentage = damageCharges.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        }
        
        return new RevenueBreakdownDto(
            rentalRevenue,
            penaltyRevenue,
            damageCharges,
            totalRevenue,
            rentalPercentage,
            penaltyPercentage,
            damagePercentage
        );
    }

    private PendingItemDto toPendingItemDto(RentalResponse rental) {
        Long lateHours = calculateLateHours(rental.endDate());
        
        return new PendingItemDto(
            rental.id(),
            rental.userSummary() != null ? rental.userSummary().username() : null,
            rental.userSummary() != null ? rental.userSummary().email() : null,
            rental.carSummary() != null ? rental.carSummary().id() : null,
            rental.carSummary() != null ? rental.carSummary().brand() : null,
            rental.carSummary() != null ? rental.carSummary().model() : null,
            rental.carSummary() != null ? rental.carSummary().licensePlate() : null,
            rental.startDate(),
            rental.endDate(),
            rental.totalPrice(),
            rental.status() != null ? rental.status().name() : null,
            lateHours,
            rental.createTime()
        );
    }

    private Long calculateLateHours(LocalDate endDate) {
        if (endDate == null || !endDate.isBefore(LocalDate.now())) {
            return null;
        }
        return java.time.temporal.ChronoUnit.HOURS.between(
            endDate.atStartOfDay(),
            LocalDateTime.now()
        );
    }
}
