package com.akif.service.damage;

import com.akif.dto.damage.request.DamageSearchFilterDto;
import com.akif.dto.damage.response.DamageReportDto;
import com.akif.dto.damage.response.DamageStatisticsDto;
import com.akif.enums.DamageCategory;
import com.akif.enums.DamageSeverity;
import com.akif.enums.DamageStatus;
import com.akif.model.*;
import com.akif.repository.DamageReportRepository;
import com.akif.service.damage.impl.DamageHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DamageHistoryService Unit Tests")
class DamageHistoryServiceTest {

    @Mock
    private DamageReportRepository damageReportRepository;

    @InjectMocks
    private DamageHistoryServiceImpl damageHistoryService;

    private User testUser;
    private Car testCar;
    private Rental testRental;
    private DamageReport testDamageReport1;
    private DamageReport testDamageReport2;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("customer")
                .email("customer@example.com")
                .build();

        testCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .build();

        testDamageReport1 = DamageReport.builder()
                .id(1L)
                .rental(testRental)
                .car(testCar)
                .description("Scratch on front bumper")
                .severity(DamageSeverity.MINOR)
                .category(DamageCategory.SCRATCH)
                .status(DamageStatus.REPORTED)
                .repairCostEstimate(new BigDecimal("500.00"))
                .customerLiability(new BigDecimal("500.00"))
                .reportedAt(LocalDateTime.now())
                .build();

        testDamageReport2 = DamageReport.builder()
                .id(2L)
                .rental(testRental)
                .car(testCar)
                .description("Dent on rear door")
                .severity(DamageSeverity.MODERATE)
                .category(DamageCategory.DENT)
                .status(DamageStatus.ASSESSED)
                .repairCostEstimate(new BigDecimal("1500.00"))
                .customerLiability(new BigDecimal("1000.00"))
                .reportedAt(LocalDateTime.now().minusDays(1))
                .build();

        pageable = PageRequest.of(0, 10);
    }

    @Nested
    @DisplayName("Get Damages By Vehicle Operations")
    class GetDamagesByVehicleOperations {

        @Test
        @DisplayName("Should get damages by vehicle successfully")
        void shouldGetDamagesByVehicleSuccessfully() {
            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1, testDamageReport2));

            when(damageReportRepository.findByCarIdAndIsDeletedFalse(1L, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByVehicle(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).carLicensePlate()).isEqualTo("34ABC123");

            verify(damageReportRepository).findByCarIdAndIsDeletedFalse(1L, pageable);
        }

        @Test
        @DisplayName("Should return empty page when no damages found for vehicle")
        void shouldReturnEmptyPageWhenNoDamagesFoundForVehicle() {
            Page<DamageReport> emptyPage = new PageImpl<>(List.of());

            when(damageReportRepository.findByCarIdAndIsDeletedFalse(999L, pageable)).thenReturn(emptyPage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByVehicle(999L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Should map damage report to DTO correctly")
        void shouldMapDamageReportToDtoCorrectly() {
            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1));

            when(damageReportRepository.findByCarIdAndIsDeletedFalse(1L, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByVehicle(1L, pageable);

            DamageReportDto dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.rentalId()).isEqualTo(1L);
            assertThat(dto.carId()).isEqualTo(1L);
            assertThat(dto.carLicensePlate()).isEqualTo("34ABC123");
            assertThat(dto.customerName()).isEqualTo("customer");
            assertThat(dto.description()).isEqualTo("Scratch on front bumper");
            assertThat(dto.severity()).isEqualTo(DamageSeverity.MINOR);
            assertThat(dto.category()).isEqualTo(DamageCategory.SCRATCH);
            assertThat(dto.status()).isEqualTo(DamageStatus.REPORTED);
            assertThat(dto.repairCostEstimate()).isEqualTo(new BigDecimal("500.00"));
            assertThat(dto.customerLiability()).isEqualTo(new BigDecimal("500.00"));
        }
    }

    @Nested
    @DisplayName("Get Damages By Customer Operations")
    class GetDamagesByCustomerOperations {

        @Test
        @DisplayName("Should get damages by customer successfully")
        void shouldGetDamagesByCustomerSuccessfully() {
            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1));

            when(damageReportRepository.findByRental_UserIdAndIsDeletedFalse(1L, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByCustomer(1L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).customerName()).isEqualTo("customer");

            verify(damageReportRepository).findByRental_UserIdAndIsDeletedFalse(1L, pageable);
        }

        @Test
        @DisplayName("Should return empty page when no damages found for customer")
        void shouldReturnEmptyPageWhenNoDamagesFoundForCustomer() {
            Page<DamageReport> emptyPage = new PageImpl<>(List.of());

            when(damageReportRepository.findByRental_UserIdAndIsDeletedFalse(999L, pageable)).thenReturn(emptyPage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByCustomer(999L, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Search Damages Operations")
    class SearchDamagesOperations {

        @Test
        @DisplayName("Should search damages with all filters")
        void shouldSearchDamagesWithAllFilters() {
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH,
                    DamageStatus.REPORTED,
                    1L,
                    1L
            );

            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1));

            when(damageReportRepository.searchDamages(
                    any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.searchDamages(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(damageReportRepository).searchDamages(
                    eq(filter.startDate()),
                    eq(filter.endDate()),
                    eq(filter.severity()),
                    eq(filter.category()),
                    eq(filter.status()),
                    eq(filter.carId()),
                    eq(filter.customerId()),
                    eq(pageable)
            );
        }

        @Test
        @DisplayName("Should search damages with partial filters")
        void shouldSearchDamagesWithPartialFilters() {
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    LocalDate.now().minusDays(30),
                    LocalDate.now(),
                    null,
                    null,
                    null,
                    null,
                    null
            );

            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1, testDamageReport2));

            when(damageReportRepository.searchDamages(
                    any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.searchDamages(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty page when no damages match filter")
        void shouldReturnEmptyPageWhenNoDamagesMatchFilter() {
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    LocalDate.now().minusDays(1),
                    LocalDate.now(),
                    DamageSeverity.TOTAL_LOSS,
                    null,
                    null,
                    null,
                    null
            );

            Page<DamageReport> emptyPage = new PageImpl<>(List.of());

            when(damageReportRepository.searchDamages(
                    any(), any(), any(), any(), any(), any(), any(), any()
            )).thenReturn(emptyPage);

            Page<DamageReportDto> result = damageHistoryService.searchDamages(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Damage Statistics Operations")
    class GetDamageStatisticsOperations {

        @Test
        @DisplayName("Should get damage statistics successfully")
        void shouldGetDamageStatisticsSuccessfully() {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(damageReportRepository.countTotalDamages(startDate, endDate)).thenReturn(10);
            when(damageReportRepository.countBySeverity(DamageSeverity.MINOR, startDate, endDate)).thenReturn(5);
            when(damageReportRepository.countBySeverity(DamageSeverity.MODERATE, startDate, endDate)).thenReturn(3);
            when(damageReportRepository.countBySeverity(DamageSeverity.MAJOR, startDate, endDate)).thenReturn(1);
            when(damageReportRepository.countBySeverity(DamageSeverity.TOTAL_LOSS, startDate, endDate)).thenReturn(1);
            when(damageReportRepository.countByStatus(DamageStatus.DISPUTED, startDate, endDate)).thenReturn(2);
            when(damageReportRepository.countByStatus(DamageStatus.RESOLVED, startDate, endDate)).thenReturn(1);
            when(damageReportRepository.sumTotalRepairCost(startDate, endDate)).thenReturn(new BigDecimal("25000.00"));
            when(damageReportRepository.sumTotalCustomerLiability(startDate, endDate)).thenReturn(new BigDecimal("15000.00"));
            when(damageReportRepository.averageRepairCost(startDate, endDate)).thenReturn(new BigDecimal("2500.00"));

            DamageStatisticsDto result = damageHistoryService.getDamageStatistics(startDate, endDate);

            assertThat(result).isNotNull();
            assertThat(result.totalDamages()).isEqualTo(10);
            assertThat(result.minorCount()).isEqualTo(5);
            assertThat(result.moderateCount()).isEqualTo(3);
            assertThat(result.majorCount()).isEqualTo(1);
            assertThat(result.totalLossCount()).isEqualTo(1);
            assertThat(result.totalRepairCost()).isEqualTo(new BigDecimal("25000.00"));
            assertThat(result.totalCustomerLiability()).isEqualTo(new BigDecimal("15000.00"));
            assertThat(result.averageRepairCost()).isEqualTo(new BigDecimal("2500.00"));
            assertThat(result.disputedCount()).isEqualTo(2);
            assertThat(result.resolvedCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return zero statistics when no damages in period")
        void shouldReturnZeroStatisticsWhenNoDamagesInPeriod() {
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();

            when(damageReportRepository.countTotalDamages(startDate, endDate)).thenReturn(0);
            when(damageReportRepository.countBySeverity(any(), any(), any())).thenReturn(0);
            when(damageReportRepository.countByStatus(any(), any(), any())).thenReturn(0);
            when(damageReportRepository.sumTotalRepairCost(startDate, endDate)).thenReturn(null);
            when(damageReportRepository.sumTotalCustomerLiability(startDate, endDate)).thenReturn(null);
            when(damageReportRepository.averageRepairCost(startDate, endDate)).thenReturn(null);

            DamageStatisticsDto result = damageHistoryService.getDamageStatistics(startDate, endDate);

            assertThat(result).isNotNull();
            assertThat(result.totalDamages()).isEqualTo(0);
            assertThat(result.minorCount()).isEqualTo(0);
            assertThat(result.moderateCount()).isEqualTo(0);
            assertThat(result.majorCount()).isEqualTo(0);
            assertThat(result.totalLossCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should call repository with correct date range")
        void shouldCallRepositoryWithCorrectDateRange() {
            LocalDate startDate = LocalDate.of(2024, 1, 1);
            LocalDate endDate = LocalDate.of(2024, 12, 31);

            when(damageReportRepository.countTotalDamages(startDate, endDate)).thenReturn(0);
            when(damageReportRepository.countBySeverity(any(), any(), any())).thenReturn(0);
            when(damageReportRepository.countByStatus(any(), any(), any())).thenReturn(0);
            when(damageReportRepository.sumTotalRepairCost(startDate, endDate)).thenReturn(BigDecimal.ZERO);
            when(damageReportRepository.sumTotalCustomerLiability(startDate, endDate)).thenReturn(BigDecimal.ZERO);
            when(damageReportRepository.averageRepairCost(startDate, endDate)).thenReturn(BigDecimal.ZERO);

            damageHistoryService.getDamageStatistics(startDate, endDate);

            verify(damageReportRepository).countTotalDamages(startDate, endDate);
            verify(damageReportRepository).countBySeverity(DamageSeverity.MINOR, startDate, endDate);
            verify(damageReportRepository).countBySeverity(DamageSeverity.MODERATE, startDate, endDate);
            verify(damageReportRepository).countBySeverity(DamageSeverity.MAJOR, startDate, endDate);
            verify(damageReportRepository).countBySeverity(DamageSeverity.TOTAL_LOSS, startDate, endDate);
            verify(damageReportRepository).countByStatus(DamageStatus.DISPUTED, startDate, endDate);
            verify(damageReportRepository).countByStatus(DamageStatus.RESOLVED, startDate, endDate);
        }
    }
}
