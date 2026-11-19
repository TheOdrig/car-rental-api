package com.akif.dashboard.unit;

import java.time.Duration;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.DailyRevenueDto;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.MonthlyMetricsDto;
import com.akif.dashboard.api.dto.MonthlyRevenueDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.api.dto.RevenueAnalyticsDto;
import com.akif.dashboard.api.dto.RevenueBreakdownDto;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.dashboard.internal.service.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Clock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardServiceImpl Unit Tests")
class DashboardServiceImplTest {

    @Mock
    private DashboardQueryService queryService;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private DailySummaryDto testDailySummary;
    private FleetStatusDto testFleetStatus;
    private MonthlyMetricsDto testMonthlyMetrics;
    private RevenueAnalyticsDto testRevenueAnalytics;
    private Page<PendingItemDto> testPendingItems;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now();

        testDailySummary = new DailySummaryDto(
                5,
                10,
                8,
                2,
                3,
                now);

        testFleetStatus = new FleetStatusDto(
                100,
                60,
                30,
                5,
                5,
                0,
                5,
                new BigDecimal("30.00"),
                now);

        RevenueBreakdownDto breakdown = new RevenueBreakdownDto(
                CurrencyType.TRY,
                new BigDecimal("140000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2500.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("-2500.00"),
                new BigDecimal("147500.00"),
                new BigDecimal("142500.00"),
                new BigDecimal("94.92"),
                new BigDecimal("3.39"),
                new BigDecimal("1.69"));

        testMonthlyMetrics = new MonthlyMetricsDto(
                List.of(breakdown),
                120,
                10,
                new BigDecimal("3.5"),
                today.withDayOfMonth(1),
                today,
                now);

        List<DailyRevenueDto> dailyRevenue = List.of(
                new DailyRevenueDto(today.minusDays(1), CurrencyType.TRY, new BigDecimal("5000.00"), 10),
                new DailyRevenueDto(today, CurrencyType.TRY, new BigDecimal("7500.00"), 15));

        List<MonthlyRevenueDto> monthlyRevenue = List.of(
                new MonthlyRevenueDto(YearMonth.of(2024, 11), CurrencyType.TRY, new BigDecimal("120000.00"), 100,
                        new BigDecimal("10.00")),
                new MonthlyRevenueDto(YearMonth.of(2024, 12), CurrencyType.TRY, new BigDecimal("150000.00"), 120,
                        new BigDecimal("25.00")));

        testRevenueAnalytics = new RevenueAnalyticsDto(
                dailyRevenue,
                monthlyRevenue,
                List.of(breakdown),
                now);

        PendingItemDto pendingItem = createPendingItemDto(1L, "testuser", "test@example.com", 100L, "Toyota", "Corolla",
                "34ABC123", today, today.plusDays(7), new BigDecimal("3500.00"), "REQUESTED", null, now);

        testPendingItems = new PageImpl<>(List.of(pendingItem));
        testPageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("Daily Summary Operations")
    class DailySummaryOperations {

        @Test
        @DisplayName("getDailySummary - should delegate to queryService correctly")
        void shouldDelegateToQueryService() {
            when(queryService.fetchDailySummary()).thenReturn(testDailySummary);

            DailySummaryDto result = dashboardService.getDailySummary();

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testDailySummary);
            verify(queryService).fetchDailySummary();
        }

        @Test
        @DisplayName("getDailySummary - should return all fields correctly")
        void shouldReturnAllFieldsCorrectly() {
            when(queryService.fetchDailySummary()).thenReturn(testDailySummary);

            DailySummaryDto result = dashboardService.getDailySummary();

            assertThat(result.pendingApprovals()).isEqualTo(5);
            assertThat(result.todaysPickups()).isEqualTo(10);
            assertThat(result.todaysReturns()).isEqualTo(8);
            assertThat(result.overdueRentals()).isEqualTo(2);
            assertThat(result.pendingDamageAssessments()).isEqualTo(3);
            assertThat(result.generatedAt()).isNotNull();
        }

        @Test
        @DisplayName("getDailySummary - should handle zero values")
        void shouldHandleZeroValues() {
            DailySummaryDto emptySummary = new DailySummaryDto(
                    0, 0, 0, 0, 0, Instant.now());
            when(queryService.fetchDailySummary()).thenReturn(emptySummary);

            DailySummaryDto result = dashboardService.getDailySummary();

            assertThat(result.pendingApprovals()).isZero();
            assertThat(result.todaysPickups()).isZero();
            assertThat(result.todaysReturns()).isZero();
            assertThat(result.overdueRentals()).isZero();
            assertThat(result.pendingDamageAssessments()).isZero();
        }
    }

    @Nested
    @DisplayName("Fleet Status Operations")
    class FleetStatusOperations {

        @Test
        @DisplayName("getFleetStatus - should delegate to queryService correctly")
        void shouldDelegateToQueryService() {
            when(queryService.fetchFleetStatus()).thenReturn(testFleetStatus);

            FleetStatusDto result = dashboardService.getFleetStatus();

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testFleetStatus);
            verify(queryService).fetchFleetStatus();
        }

        @Test
        @DisplayName("getFleetStatus - should return correct occupancy rate")
        void shouldReturnCorrectOccupancyRate() {
            when(queryService.fetchFleetStatus()).thenReturn(testFleetStatus);

            FleetStatusDto result = dashboardService.getFleetStatus();

            assertThat(result.totalCars()).isEqualTo(100);
            assertThat(result.rentedCars()).isEqualTo(30);
            assertThat(result.availableCars()).isEqualTo(60);
            assertThat(result.occupancyRate()).isEqualByComparingTo(new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("getFleetStatus - should handle zero cars")
        void shouldHandleZeroCars() {
            FleetStatusDto emptyFleet = new FleetStatusDto(
                    0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO, Instant.now());
            when(queryService.fetchFleetStatus()).thenReturn(emptyFleet);

            FleetStatusDto result = dashboardService.getFleetStatus();

            assertThat(result.totalCars()).isZero();
            assertThat(result.occupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("getFleetStatus - should include all car statuses")
        void shouldIncludeAllCarStatuses() {
            when(queryService.fetchFleetStatus()).thenReturn(testFleetStatus);

            FleetStatusDto result = dashboardService.getFleetStatus();

            assertThat(result.availableCars()).isEqualTo(60);
            assertThat(result.rentedCars()).isEqualTo(30);
            assertThat(result.maintenanceCars()).isEqualTo(5);
            assertThat(result.damagedCars()).isEqualTo(5);
            int calculatedTotal = result.availableCars() + result.rentedCars()
                    + result.maintenanceCars() + result.damagedCars();
            assertThat(calculatedTotal).isEqualTo(result.totalCars());
        }
    }

    @Nested
    @DisplayName("Monthly Metrics Operations")
    class MonthlyMetricsOperations {

        @Test
        @DisplayName("getMonthlyMetrics() - should use default date range for current month")
        void shouldUseDefaultDateRangeForCurrentMonth() {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            when(queryService.fetchMonthlyMetrics(startOfMonth, today)).thenReturn(testMonthlyMetrics);

            MonthlyMetricsDto result = dashboardService.getMonthlyMetrics();

            assertThat(result).isNotNull();
            verify(queryService).fetchMonthlyMetrics(startOfMonth, today);
        }

        @Test
        @DisplayName("getMonthlyMetrics(startDate, endDate) - should use specified date range")
        void shouldUseSpecifiedDateRange() {
            LocalDate startDate = LocalDate.of(2024, 11, 1);
            LocalDate endDate = LocalDate.of(2024, 11, 30);
            when(queryService.fetchMonthlyMetrics(startDate, endDate)).thenReturn(testMonthlyMetrics);

            MonthlyMetricsDto result = dashboardService.getMonthlyMetrics(startDate, endDate);

            assertThat(result).isNotNull();
            verify(queryService).fetchMonthlyMetrics(startDate, endDate);
        }

        @Test
        @DisplayName("getMonthlyMetrics - should return all metrics correctly")
        void shouldReturnAllMetricsCorrectly() {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            when(queryService.fetchMonthlyMetrics(startOfMonth, today)).thenReturn(testMonthlyMetrics);

            MonthlyMetricsDto result = dashboardService.getMonthlyMetrics();

            assertThat(result.revenueByCurrency()).hasSize(1);
            assertThat(result.revenueByCurrency().get(0).totalRevenue()).isEqualByComparingTo(new BigDecimal("147500.00"));
            assertThat(result.completedRentals()).isEqualTo(120);
            assertThat(result.cancelledRentals()).isEqualTo(10);
            assertThat(result.revenueByCurrency().get(0).penaltyRevenue()).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(result.revenueByCurrency().get(0).damageRecovered()).isEqualByComparingTo(new BigDecimal("2500.00"));
            assertThat(result.averageRentalDurationDays()).isEqualByComparingTo(new BigDecimal("3.5"));
        }

        @Test
        @DisplayName("getMonthlyMetrics - should handle zero revenue")
        void shouldHandleZeroRevenue() {
            LocalDate today = LocalDate.now();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            MonthlyMetricsDto emptyMetrics = new MonthlyMetricsDto(
                    List.of(), 0, 0, BigDecimal.ZERO, startOfMonth, today, Instant.now());
            when(queryService.fetchMonthlyMetrics(startOfMonth, today)).thenReturn(emptyMetrics);

            MonthlyMetricsDto result = dashboardService.getMonthlyMetrics();

            assertThat(result.revenueByCurrency()).isEmpty();
            assertThat(result.completedRentals()).isZero();
        }
    }

    @Nested
    @DisplayName("Revenue Analytics Operations")
    class RevenueAnalyticsOperations {

        @Test
        @DisplayName("getRevenueAnalytics - should delegate to queryService correctly")
        void shouldDelegateToQueryService() {
            when(queryService.fetchRevenueAnalytics()).thenReturn(testRevenueAnalytics);

            RevenueAnalyticsDto result = dashboardService.getRevenueAnalytics();

            assertThat(result).isNotNull();
            assertThat(result).isEqualTo(testRevenueAnalytics);
            verify(queryService).fetchRevenueAnalytics();
        }

        @Test
        @DisplayName("getRevenueAnalytics - should contain daily and monthly revenue data")
        void shouldContainDailyAndMonthlyRevenue() {
            when(queryService.fetchRevenueAnalytics()).thenReturn(testRevenueAnalytics);

            RevenueAnalyticsDto result = dashboardService.getRevenueAnalytics();

            assertThat(result.dailyRevenue()).hasSize(2);
            assertThat(result.monthlyRevenue()).hasSize(2);
            assertThat(result.breakdownByCurrency()).isNotEmpty();
        }

        @Test
        @DisplayName("getRevenueAnalytics - breakdown percentages should total 100")
        void shouldHaveBreakdownPercentagesTotalHundred() {
            when(queryService.fetchRevenueAnalytics()).thenReturn(testRevenueAnalytics);

            RevenueAnalyticsDto result = dashboardService.getRevenueAnalytics();

            RevenueBreakdownDto breakdown = result.breakdownByCurrency().get(0);
            BigDecimal totalPercentage = breakdown.rentalPercentage()
                    .add(breakdown.penaltyPercentage())
                    .add(breakdown.damagePercentage());
            assertThat(totalPercentage).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("getRevenueAnalytics - should handle empty data")
        void shouldHandleEmptyData() {
            RevenueAnalyticsDto emptyAnalytics = new RevenueAnalyticsDto(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    List.of(),
                    Instant.now());
            when(queryService.fetchRevenueAnalytics()).thenReturn(emptyAnalytics);

            RevenueAnalyticsDto result = dashboardService.getRevenueAnalytics();

            assertThat(result.dailyRevenue()).isEmpty();
            assertThat(result.monthlyRevenue()).isEmpty();
            assertThat(result.breakdownByCurrency()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Pending Items Operations (Real-time, No Cache)")
    class PendingItemsOperations {

        @Test
        @DisplayName("getPendingApprovals - should query with pageable parameter")
        void shouldQueryWithPageable() {
            when(queryService.fetchPendingApprovals(testPageable)).thenReturn(testPendingItems);

            Page<PendingItemDto> result = dashboardService.getPendingApprovals(testPageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            verify(queryService).fetchPendingApprovals(testPageable);
        }

        @Test
        @DisplayName("getTodaysPickups - should return today's pickups")
        void shouldReturnTodaysPickups() {
            when(queryService.fetchTodaysPickups(testPageable)).thenReturn(testPendingItems);

            Page<PendingItemDto> result = dashboardService.getTodaysPickups(testPageable);

            assertThat(result).isNotNull();
            verify(queryService).fetchTodaysPickups(testPageable);
        }

        @Test
        @DisplayName("getTodaysReturns - should return today's returns")
        void shouldReturnTodaysReturns() {
            when(queryService.fetchTodaysReturns(testPageable)).thenReturn(testPendingItems);

            Page<PendingItemDto> result = dashboardService.getTodaysReturns(testPageable);

            assertThat(result).isNotNull();
            verify(queryService).fetchTodaysReturns(testPageable);
        }

        @Test
        @DisplayName("getOverdueRentals - should return overdue rentals")
        void shouldReturnOverdueRentals() {
            PendingItemDto overdueItem = createPendingItemDto(2L, "lateuser", "late@example.com", 200L, "BMW", "320i",
                    "35XYZ789", LocalDate.now().minusDays(10), LocalDate.now().minusDays(3), new BigDecimal("5000.00"),
                    "IN_USE", 72L, Instant.now().minus(Duration.ofDays(10)));
            Page<PendingItemDto> overdueItems = new PageImpl<>(List.of(overdueItem));
            when(queryService.fetchOverdueRentals(testPageable)).thenReturn(overdueItems);

            Page<PendingItemDto> result = dashboardService.getOverdueRentals(testPageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).lateHours()).isEqualTo(72L);
            verify(queryService).fetchOverdueRentals(testPageable);
        }

        @Test
        @DisplayName("getPendingApprovals - should return empty page")
        void shouldReturnEmptyPage() {
            Page<PendingItemDto> emptyPage = Page.empty(testPageable);
            when(queryService.fetchPendingApprovals(testPageable)).thenReturn(emptyPage);

            Page<PendingItemDto> result = dashboardService.getPendingApprovals(testPageable);

            assertThat(result).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @Test
        @DisplayName("getPendingApprovals - should support multiple pages")
        void shouldSupportMultiplePages() {
            Pageable secondPage = PageRequest.of(1, 10);
            Page<PendingItemDto> secondPageResult = new PageImpl<>(
                    List.of(testPendingItems.getContent().get(0)),
                    secondPage,
                    25);
            when(queryService.fetchPendingApprovals(secondPage)).thenReturn(secondPageResult);

            Page<PendingItemDto> result = dashboardService.getPendingApprovals(secondPage);

            assertThat(result.getNumber()).isEqualTo(1);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("PendingItemDto Field Validation")
    class PendingItemDtoFieldValidation {

        @Test
        @DisplayName("PendingItemDto - should return all fields with correct values")
        void shouldReturnCorrectFields() {
            when(queryService.fetchPendingApprovals(testPageable)).thenReturn(testPendingItems);

            Page<PendingItemDto> result = dashboardService.getPendingApprovals(testPageable);
            PendingItemDto item = result.getContent().get(0);

            assertThat(item.rentalId()).isEqualTo(1L);
            assertThat(item.customerName()).isEqualTo("testuser");
            assertThat(item.customerEmail()).isEqualTo("test@example.com");
            assertThat(item.carId()).isEqualTo(100L);
            assertThat(item.carBrand()).isEqualTo("Toyota");
            assertThat(item.carModel()).isEqualTo("Corolla");
            assertThat(item.licensePlate()).isEqualTo("34ABC123");
            assertThat(item.totalAmount()).isEqualByComparingTo(new BigDecimal("3500.00"));
            assertThat(item.status()).isEqualTo("REQUESTED");
            assertThat(item.lateHours()).isNull();
        }

        @Test
        @DisplayName("PendingItemDto - should handle null fields")
        void shouldHandleNullFields() {
            PendingItemDto itemWithNulls = createPendingItemDto(1L, null, null, null, null, null, null, null, null,
                    null, null, null, null);
            Page<PendingItemDto> pageWithNulls = new PageImpl<>(List.of(itemWithNulls));
            when(queryService.fetchPendingApprovals(testPageable)).thenReturn(pageWithNulls);

            Page<PendingItemDto> result = dashboardService.getPendingApprovals(testPageable);
            PendingItemDto item = result.getContent().get(0);

            assertThat(item.rentalId()).isEqualTo(1L);
            assertThat(item.customerName()).isNull();
            assertThat(item.carBrand()).isNull();
        }
    }

    private PendingItemDto createPendingItemDto(Long rentalId, String customerName, String customerEmail, Long carId,
            String carBrand, String carModel, String licensePlate, LocalDate startDate, LocalDate endDate,
            BigDecimal totalAmount, String status, Long lateHours, Instant createdAt) {
        return new PendingItemDto(
                rentalId,
                10L,
                customerName,
                customerEmail,
                carId,
                carBrand,
                carModel,
                licensePlate,
                "default-image-url",
                startDate,
                endDate,
                totalAmount,
                com.akif.shared.enums.CurrencyType.TRY,
                status,
                lateHours,
                createdAt);
    }
}
