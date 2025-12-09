package com.akif.service.report;

import com.akif.dto.report.LateReturnFilterDto;
import com.akif.dto.report.LateReturnReportDto;
import com.akif.dto.report.LateReturnStatisticsDto;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.LateReturnStatus;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.RentalRepository;
import com.akif.service.report.impl.LateReturnReportServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LateReturnReportServiceImpl Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class LateReturnReportServiceImplTest {

    @Mock
    private RentalRepository rentalRepository;

    @InjectMocks
    private LateReturnReportServiceImpl service;

    private Rental testRental1;
    private Rental testRental2;
    private User testUser;
    private Car testCar;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .build();

        testCar = Car.builder()
                .id(1L)
                .brand("Toyota")
                .model("Corolla")
                .licensePlate("34ABC123")
                .build();

        testRental1 = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(5))
                .endDate(LocalDate.now().minusDays(2))
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .lateReturnStatus(LateReturnStatus.LATE)
                .lateHours(5)
                .penaltyAmount(new BigDecimal("100.00"))
                .penaltyPaid(false)
                .actualReturnTime(LocalDateTime.now().minusDays(1))
                .build();

        testRental2 = Rental.builder()
                .id(2L)
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().minusDays(10))
                .endDate(LocalDate.now().minusDays(5))
                .dailyPrice(new BigDecimal("600.00"))
                .currency(CurrencyType.TRY)
                .lateReturnStatus(LateReturnStatus.SEVERELY_LATE)
                .lateHours(48)
                .penaltyAmount(new BigDecimal("500.00"))
                .penaltyPaid(true)
                .actualReturnTime(LocalDateTime.now().minusDays(3))
                .build();
    }

    @Nested
    @DisplayName("Filtering by Date Range")
    class FilteringByDateRange {

        @Test
        @DisplayName("Should filter late returns by date range")
        void shouldFilterLateReturnsByDateRange() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(10);
            LocalDate endDate = LocalDate.now();
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(testRental1, testRental2));
            when(rentalRepository.findLateReturns(anyList(), eq(startDate), eq(endDate), any(Pageable.class)))
                    .thenReturn(page);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(2);
            verify(rentalRepository, times(1)).findLateReturns(anyList(), eq(startDate), eq(endDate), any(Pageable.class));
        }

        @Test
        @DisplayName("Should return empty page when no late returns in date range")
        void shouldReturnEmptyPageWhenNoLateReturnsInDateRange() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now().minusDays(20);
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            Page<Rental> emptyPage = new PageImpl<>(Collections.emptyList());
            when(rentalRepository.findLateReturns(anyList(), eq(startDate), eq(endDate), any(Pageable.class)))
                    .thenReturn(emptyPage);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should filter by specific status")
        void shouldFilterBySpecificStatus() {
            // Given
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .status(LateReturnStatus.SEVERELY_LATE)
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .build();

            Page<Rental> page = new PageImpl<>(Collections.singletonList(testRental2));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).status()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        }
    }

    @Nested
    @DisplayName("Sorting Options")
    class SortingOptions {

        @Test
        @DisplayName("Should sort by end date descending by default")
        void shouldSortByEndDateDescendingByDefault() {
            // Given
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(testRental1, testRental2));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            verify(rentalRepository, times(1)).findLateReturns(
                    anyList(),
                    any(LocalDate.class),
                    any(LocalDate.class),
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("Should sort by penalty amount ascending")
        void shouldSortByPenaltyAmountAscending() {
            // Given
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .sortBy("penaltyAmount")
                    .sortDirection("ASC")
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(testRental1, testRental2));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            verify(rentalRepository, times(1)).findLateReturns(
                    anyList(),
                    any(LocalDate.class),
                    any(LocalDate.class),
                    any(Pageable.class)
            );
        }

        @Test
        @DisplayName("Should sort by late hours descending")
        void shouldSortByLateHoursDescending() {
            // Given
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .sortBy("lateHours")
                    .sortDirection("DESC")
                    .build();

            Page<Rental> page = new PageImpl<>(Arrays.asList(testRental2, testRental1));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent().get(0).lateHours()).isEqualTo(48);
            assertThat(result.getContent().get(1).lateHours()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Statistics Calculation")
    class StatisticsCalculation {

        @Test
        @DisplayName("Should calculate statistics correctly")
        void shouldCalculateStatisticsCorrectly() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(rentalRepository.countLateReturns(anyList(), eq(startDate), eq(endDate))).thenReturn(10L);
            when(rentalRepository.countSeverelyLateReturns(eq(startDate), eq(endDate))).thenReturn(3L);
            when(rentalRepository.sumTotalPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("1000.00"));
            when(rentalRepository.sumCollectedPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("600.00"));
            when(rentalRepository.averageLateHours(anyList(), eq(startDate), eq(endDate))).thenReturn(12.5);
            when(rentalRepository.countTotalReturns(eq(startDate), eq(endDate))).thenReturn(100L);

            // When
            LateReturnStatisticsDto result = service.getStatistics(startDate, endDate);

            // Then
            assertThat(result.totalLateReturns()).isEqualTo(10);
            assertThat(result.severelyLateCount()).isEqualTo(3);
            assertThat(result.totalPenaltyAmount()).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(result.collectedPenaltyAmount()).isEqualByComparingTo(new BigDecimal("600.00"));
            assertThat(result.pendingPenaltyAmount()).isEqualByComparingTo(new BigDecimal("400.00"));
            assertThat(result.averageLateHours()).isEqualTo(12.5);
            assertThat(result.lateReturnPercentage()).isEqualTo(10.0);
        }

        @Test
        @DisplayName("Should handle zero total returns for percentage calculation")
        void shouldHandleZeroTotalReturnsForPercentageCalculation() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(rentalRepository.countLateReturns(anyList(), eq(startDate), eq(endDate))).thenReturn(0L);
            when(rentalRepository.countSeverelyLateReturns(eq(startDate), eq(endDate))).thenReturn(0L);
            when(rentalRepository.sumTotalPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(rentalRepository.sumCollectedPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(BigDecimal.ZERO);
            when(rentalRepository.averageLateHours(anyList(), eq(startDate), eq(endDate))).thenReturn(0.0);
            when(rentalRepository.countTotalReturns(eq(startDate), eq(endDate))).thenReturn(0L);

            // When
            LateReturnStatisticsDto result = service.getStatistics(startDate, endDate);

            // Then
            assertThat(result.lateReturnPercentage()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should calculate pending penalty amount correctly")
        void shouldCalculatePendingPenaltyAmountCorrectly() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(rentalRepository.countLateReturns(anyList(), eq(startDate), eq(endDate))).thenReturn(5L);
            when(rentalRepository.countSeverelyLateReturns(eq(startDate), eq(endDate))).thenReturn(2L);
            when(rentalRepository.sumTotalPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("2000.00"));
            when(rentalRepository.sumCollectedPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("1500.00"));
            when(rentalRepository.averageLateHours(anyList(), eq(startDate), eq(endDate))).thenReturn(15.0);
            when(rentalRepository.countTotalReturns(eq(startDate), eq(endDate))).thenReturn(50L);

            // When
            LateReturnStatisticsDto result = service.getStatistics(startDate, endDate);

            // Then
            assertThat(result.pendingPenaltyAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("Should calculate percentage with two decimal places")
        void shouldCalculatePercentageWithTwoDecimalPlaces() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(rentalRepository.countLateReturns(anyList(), eq(startDate), eq(endDate))).thenReturn(7L);
            when(rentalRepository.countSeverelyLateReturns(eq(startDate), eq(endDate))).thenReturn(2L);
            when(rentalRepository.sumTotalPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("500.00"));
            when(rentalRepository.sumCollectedPenaltyAmount(anyList(), eq(startDate), eq(endDate)))
                    .thenReturn(new BigDecimal("300.00"));
            when(rentalRepository.averageLateHours(anyList(), eq(startDate), eq(endDate))).thenReturn(10.0);
            when(rentalRepository.countTotalReturns(eq(startDate), eq(endDate))).thenReturn(30L);

            // When
            LateReturnStatisticsDto result = service.getStatistics(startDate, endDate);

            // Then
            assertThat(result.lateReturnPercentage()).isEqualTo(23.33);
        }
    }

    @Nested
    @DisplayName("Report DTO Mapping")
    class ReportDtoMapping {

        @Test
        @DisplayName("Should map rental to report DTO correctly")
        void shouldMapRentalToReportDtoCorrectly() {
            // Given
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .build();

            Page<Rental> page = new PageImpl<>(Collections.singletonList(testRental1));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            LateReturnReportDto dto = result.getContent().get(0);
            assertThat(dto.rentalId()).isEqualTo(1L);
            assertThat(dto.customerName()).isEqualTo("testuser");
            assertThat(dto.customerEmail()).isEqualTo("test@example.com");
            assertThat(dto.carBrand()).isEqualTo("Toyota");
            assertThat(dto.carModel()).isEqualTo("Corolla");
            assertThat(dto.licensePlate()).isEqualTo("34ABC123");
            assertThat(dto.lateHours()).isEqualTo(5);
            assertThat(dto.lateDays()).isEqualTo(0);
            assertThat(dto.status()).isEqualTo(LateReturnStatus.LATE);
            assertThat(dto.penaltyAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
            assertThat(dto.currency()).isEqualTo(CurrencyType.TRY);
            assertThat(dto.penaltyPaid()).isFalse();
        }

        @Test
        @DisplayName("Should calculate late days from late hours")
        void shouldCalculateLateDaysFromLateHours() {
            // Given
            testRental2.setLateHours(72); // 3 days
            LateReturnFilterDto filter = LateReturnFilterDto.builder()
                    .startDate(LocalDate.now().minusDays(10))
                    .endDate(LocalDate.now())
                    .build();

            Page<Rental> page = new PageImpl<>(Collections.singletonList(testRental2));
            when(rentalRepository.findLateReturns(anyList(), any(LocalDate.class), any(LocalDate.class), any(Pageable.class)))
                    .thenReturn(page);

            // When
            Page<LateReturnReportDto> result = service.getLateReturns(filter, PageRequest.of(0, 10));

            // Then
            LateReturnReportDto dto = result.getContent().get(0);
            assertThat(dto.lateDays()).isEqualTo(3);
        }
    }
}
