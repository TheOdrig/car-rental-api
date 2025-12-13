package com.akif.damage.unit;

import com.akif.damage.api.DamageReportDto;
import com.akif.damage.domain.enums.DamageCategory;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.request.DamageSearchFilterDto;
import com.akif.damage.internal.dto.damage.response.DamageStatisticsDto;
import com.akif.damage.internal.mapper.DamageMapper;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.service.damage.impl.DamageHistoryServiceImpl;
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

    @Mock
    private DamageMapper damageMapper;

    @InjectMocks
    private DamageHistoryServiceImpl damageHistoryService;

    private DamageReport testDamageReport1;
    private DamageReport testDamageReport2;
    private DamageReportDto testDto1;
    private DamageReportDto testDto2;
    private Pageable pageable;

    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_USER_NAME = "customer";
    private static final String TEST_USER_EMAIL = "customer@example.com";
    private static final Long TEST_CAR_ID = 1L;
    private static final String TEST_CAR_LICENSE_PLATE = "34ABC123";
    private static final String TEST_CAR_BRAND = "Toyota";
    private static final String TEST_CAR_MODEL = "Corolla";
    private static final Long TEST_RENTAL_ID = 1L;

    @BeforeEach
    void setUp() {
        testDamageReport1 = DamageReport.builder()
                .id(1L)
                .rentalId(TEST_RENTAL_ID)
                .carId(TEST_CAR_ID)
                .carBrand(TEST_CAR_BRAND)
                .carModel(TEST_CAR_MODEL)
                .carLicensePlate(TEST_CAR_LICENSE_PLATE)
                .customerEmail(TEST_USER_EMAIL)
                .customerFullName(TEST_USER_NAME)
                .customerUserId(TEST_USER_ID)
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
                .rentalId(TEST_RENTAL_ID)
                .carId(TEST_CAR_ID)
                .carBrand(TEST_CAR_BRAND)
                .carModel(TEST_CAR_MODEL)
                .carLicensePlate(TEST_CAR_LICENSE_PLATE)
                .customerEmail(TEST_USER_EMAIL)
                .customerFullName(TEST_USER_NAME)
                .customerUserId(TEST_USER_ID)
                .description("Dent on rear door")
                .severity(DamageSeverity.MODERATE)
                .category(DamageCategory.DENT)
                .status(DamageStatus.ASSESSED)
                .repairCostEstimate(new BigDecimal("1500.00"))
                .customerLiability(new BigDecimal("1000.00"))
                .reportedAt(LocalDateTime.now().minusDays(1))
                .build();

        testDto1 = new DamageReportDto(
                1L,
                TEST_RENTAL_ID,
                TEST_CAR_ID,
                TEST_CAR_LICENSE_PLATE,
                TEST_USER_NAME,
                "Scratch on front bumper",
                null,
                DamageSeverity.MINOR,
                DamageCategory.SCRATCH,
                DamageStatus.REPORTED,
                new BigDecimal("500.00"),
                new BigDecimal("500.00"),
                null,
                LocalDateTime.now(),
                null
        );

        testDto2 = new DamageReportDto(
                2L,
                TEST_RENTAL_ID,
                TEST_CAR_ID,
                TEST_CAR_LICENSE_PLATE,
                TEST_USER_NAME,
                "Dent on rear door",
                null,
                DamageSeverity.MODERATE,
                DamageCategory.DENT,
                DamageStatus.ASSESSED,
                new BigDecimal("1500.00"),
                new BigDecimal("1000.00"),
                null,
                LocalDateTime.now().minusDays(1),
                null
        );

        pageable = PageRequest.of(0, 10);

        when(damageMapper.toPublicDto(testDamageReport1)).thenReturn(testDto1);
        when(damageMapper.toPublicDto(testDamageReport2)).thenReturn(testDto2);
    }

    @Nested
    @DisplayName("Get Damages By Vehicle Operations")
    class GetDamagesByVehicleOperations {

        @Test
        @DisplayName("Should get damages by vehicle successfully")
        void shouldGetDamagesByVehicleSuccessfully() {
            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1, testDamageReport2));

            when(damageReportRepository.findByCarIdAndIsDeletedFalse(TEST_CAR_ID, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByVehicle(TEST_CAR_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).carLicensePlate()).isEqualTo(TEST_CAR_LICENSE_PLATE);

            verify(damageReportRepository).findByCarIdAndIsDeletedFalse(TEST_CAR_ID, pageable);
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

            when(damageReportRepository.findByCarIdAndIsDeletedFalse(TEST_CAR_ID, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByVehicle(TEST_CAR_ID, pageable);

            DamageReportDto dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.rentalId()).isEqualTo(TEST_RENTAL_ID);
            assertThat(dto.carId()).isEqualTo(TEST_CAR_ID);
            assertThat(dto.carLicensePlate()).isEqualTo(TEST_CAR_LICENSE_PLATE);
            assertThat(dto.customerName()).isEqualTo(TEST_USER_NAME);
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

            when(damageReportRepository.findByCustomerUserIdAndIsDeletedFalse(TEST_USER_ID, pageable)).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.getDamagesByCustomer(TEST_USER_ID, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).customerName()).isEqualTo(TEST_USER_NAME);

            verify(damageReportRepository).findByCustomerUserIdAndIsDeletedFalse(TEST_USER_ID, pageable);
        }

        @Test
        @DisplayName("Should return empty page when no damages found for customer")
        void shouldReturnEmptyPageWhenNoDamagesFoundForCustomer() {
            Page<DamageReport> emptyPage = new PageImpl<>(List.of());

            when(damageReportRepository.findByCustomerUserIdAndIsDeletedFalse(999L, pageable)).thenReturn(emptyPage);

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
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    startDate,
                    endDate,
                    DamageSeverity.MINOR,
                    DamageCategory.SCRATCH,
                    DamageStatus.REPORTED,
                    TEST_CAR_ID
            );

            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1));

            when(damageReportRepository.searchDamages(
                    startDate, endDate, DamageSeverity.MINOR, DamageCategory.SCRATCH,
                    DamageStatus.REPORTED, TEST_CAR_ID, pageable
            )).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.searchDamages(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Should search damages with partial filters")
        void shouldSearchDamagesWithPartialFilters() {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    startDate,
                    endDate,
                    null,
                    null,
                    null,
                    null
            );

            Page<DamageReport> damagePage = new PageImpl<>(List.of(testDamageReport1, testDamageReport2));

            when(damageReportRepository.searchDamages(
                    startDate, endDate, null, null, null, null, pageable
            )).thenReturn(damagePage);

            Page<DamageReportDto> result = damageHistoryService.searchDamages(filter, pageable);

            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("Should return empty page when no damages match filter")
        void shouldReturnEmptyPageWhenNoDamagesMatchFilter() {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            DamageSearchFilterDto filter = new DamageSearchFilterDto(
                    startDate,
                    endDate,
                    DamageSeverity.TOTAL_LOSS,
                    null,
                    null,
                    null
            );

            Page<DamageReport> emptyPage = new PageImpl<>(List.of());

            when(damageReportRepository.searchDamages(
                    startDate, endDate, DamageSeverity.TOTAL_LOSS, null, null, null, pageable
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
            when(damageReportRepository.countBySeverity(DamageSeverity.MINOR, startDate, endDate)).thenReturn(4);
            when(damageReportRepository.countBySeverity(DamageSeverity.MODERATE, startDate, endDate)).thenReturn(3);
            when(damageReportRepository.countBySeverity(DamageSeverity.MAJOR, startDate, endDate)).thenReturn(2);
            when(damageReportRepository.countBySeverity(DamageSeverity.TOTAL_LOSS, startDate, endDate)).thenReturn(1);
            when(damageReportRepository.countByStatus(DamageStatus.DISPUTED, startDate, endDate)).thenReturn(2);
            when(damageReportRepository.countByStatus(DamageStatus.RESOLVED, startDate, endDate)).thenReturn(5);
            when(damageReportRepository.sumTotalRepairCost(startDate, endDate)).thenReturn(new BigDecimal("25000.00"));
            when(damageReportRepository.sumTotalCustomerLiability(startDate, endDate)).thenReturn(new BigDecimal("15000.00"));
            when(damageReportRepository.averageRepairCost(startDate, endDate)).thenReturn(new BigDecimal("2500.00"));

            DamageStatisticsDto result = damageHistoryService.getDamageStatistics(startDate, endDate);

            assertThat(result).isNotNull();
            assertThat(result.totalDamages()).isEqualTo(10);
            assertThat(result.minorCount()).isEqualTo(4);
            assertThat(result.moderateCount()).isEqualTo(3);
            assertThat(result.majorCount()).isEqualTo(2);
            assertThat(result.totalLossCount()).isEqualTo(1);
            assertThat(result.totalRepairCost()).isEqualTo(new BigDecimal("25000.00"));
            assertThat(result.totalCustomerLiability()).isEqualTo(new BigDecimal("15000.00"));
            assertThat(result.averageRepairCost()).isEqualTo(new BigDecimal("2500.00"));
            assertThat(result.disputedCount()).isEqualTo(2);
            assertThat(result.resolvedCount()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return zero statistics when no damages exist")
        void shouldReturnZeroStatisticsWhenNoDamagesExist() {
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();

            when(damageReportRepository.countTotalDamages(startDate, endDate)).thenReturn(0);
            when(damageReportRepository.countBySeverity(any(), eq(startDate), eq(endDate))).thenReturn(0);
            when(damageReportRepository.countByStatus(any(), eq(startDate), eq(endDate))).thenReturn(0);
            when(damageReportRepository.sumTotalRepairCost(startDate, endDate)).thenReturn(BigDecimal.ZERO);
            when(damageReportRepository.sumTotalCustomerLiability(startDate, endDate)).thenReturn(BigDecimal.ZERO);
            when(damageReportRepository.averageRepairCost(startDate, endDate)).thenReturn(BigDecimal.ZERO);

            DamageStatisticsDto result = damageHistoryService.getDamageStatistics(startDate, endDate);

            assertThat(result).isNotNull();
            assertThat(result.totalDamages()).isZero();
        }
    }
}
