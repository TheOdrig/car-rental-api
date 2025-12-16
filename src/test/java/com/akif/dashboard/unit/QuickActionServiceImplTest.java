package com.akif.dashboard.unit;

import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.QuickActionResultDto;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.dashboard.internal.service.QuickActionServiceImpl;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.domain.enums.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuickActionServiceImpl Unit Tests")
class QuickActionServiceImplTest {

    @Mock
    private RentalService rentalService;

    @Mock
    private DashboardQueryService dashboardQueryService;

    @InjectMocks
    private QuickActionServiceImpl quickActionService;

    private DailySummaryDto testSummary;
    private RentalResponse confirmedRentalResponse;
    private RentalResponse inUseRentalResponse;
    private RentalResponse returnedRentalResponse;

    @BeforeEach
    void setUp() {
        testSummary = new DailySummaryDto(
            5,
            3,
            2,
            1,
            0,
            LocalDateTime.now()
        );

        confirmedRentalResponse = createRentalResponse(RentalStatus.CONFIRMED);
        inUseRentalResponse = createRentalResponse(RentalStatus.IN_USE);
        returnedRentalResponse = createRentalResponse(RentalStatus.RETURNED);
    }

    private RentalResponse createRentalResponse(RentalStatus status) {
        return new RentalResponse(
            1L, null, null, null, null, null, null, null, null,
            status,
            null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    @Nested
    @DisplayName("Approve Rental Tests")
    class ApproveRentalTests {

        @Test
        @DisplayName("Should approve rental successfully")
        void shouldApproveRentalSuccessfully() {
            Long rentalId = 1L;
            when(rentalService.confirmRental(rentalId)).thenReturn(confirmedRentalResponse);
            when(dashboardQueryService.fetchDailySummary()).thenReturn(testSummary);

            QuickActionResultDto result = quickActionService.approveRental(rentalId);

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Rental approved successfully");
            assertThat(result.newStatus()).isEqualTo("CONFIRMED");
            assertThat(result.updatedSummary()).isEqualTo(testSummary);
            
            verify(rentalService).confirmRental(rentalId);
            verify(dashboardQueryService).fetchDailySummary();
        }

        @Test
        @DisplayName("Should throw exception when rental approval fails")
        void shouldThrowExceptionWhenApprovalFails() {
            Long rentalId = 999L;
            String errorMessage = "Rental not found";
            when(rentalService.confirmRental(rentalId)).thenThrow(new RuntimeException(errorMessage));

            assertThatThrownBy(() -> quickActionService.approveRental(rentalId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(errorMessage);
        }
    }

    @Nested
    @DisplayName("Process Pickup Tests")
    class ProcessPickupTests {

        @Test
        @DisplayName("Should process pickup successfully")
        void shouldProcessPickupSuccessfully() {
            Long rentalId = 1L;
            when(rentalService.pickupRental(eq(rentalId), anyString())).thenReturn(inUseRentalResponse);
            when(dashboardQueryService.fetchDailySummary()).thenReturn(testSummary);

            QuickActionResultDto result = quickActionService.processPickup(rentalId);

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Pickup processed successfully");
            assertThat(result.newStatus()).isEqualTo("IN_USE");
            assertThat(result.updatedSummary()).isEqualTo(testSummary);
            
            verify(rentalService).pickupRental(eq(rentalId), anyString());
            verify(dashboardQueryService).fetchDailySummary();
        }

        @Test
        @DisplayName("Should throw exception when pickup processing fails")
        void shouldThrowExceptionWhenPickupFails() {
            Long rentalId = 999L;
            String errorMessage = "Invalid rental state for pickup";
            when(rentalService.pickupRental(eq(rentalId), anyString()))
                .thenThrow(new RuntimeException(errorMessage));

            assertThatThrownBy(() -> quickActionService.processPickup(rentalId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(errorMessage);
        }
    }

    @Nested
    @DisplayName("Process Return Tests")
    class ProcessReturnTests {

        @Test
        @DisplayName("Should process return successfully")
        void shouldProcessReturnSuccessfully() {
            Long rentalId = 1L;
            when(rentalService.returnRental(eq(rentalId), anyString())).thenReturn(returnedRentalResponse);
            when(dashboardQueryService.fetchDailySummary()).thenReturn(testSummary);

            QuickActionResultDto result = quickActionService.processReturn(rentalId);

            assertThat(result.success()).isTrue();
            assertThat(result.message()).isEqualTo("Return processed successfully");
            assertThat(result.newStatus()).isEqualTo("RETURNED");
            assertThat(result.updatedSummary()).isEqualTo(testSummary);
            
            verify(rentalService).returnRental(eq(rentalId), anyString());
            verify(dashboardQueryService).fetchDailySummary();
        }

        @Test
        @DisplayName("Should throw exception when return processing fails")
        void shouldThrowExceptionWhenReturnFails() {
            Long rentalId = 999L;
            String errorMessage = "Invalid rental state for return";
            when(rentalService.returnRental(eq(rentalId), anyString()))
                .thenThrow(new RuntimeException(errorMessage));

            assertThatThrownBy(() -> quickActionService.processReturn(rentalId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining(errorMessage);
        }
    }

    @Nested
    @DisplayName("Updated Summary Tests")
    class UpdatedSummaryTests {

        @Test
        @DisplayName("Should return fresh summary after each action")
        void shouldReturnFreshSummaryAfterEachAction() {
            Long rentalId = 1L;
            DailySummaryDto initialSummary = new DailySummaryDto(10, 5, 3, 2, 1, LocalDateTime.now());
            DailySummaryDto updatedSummary = new DailySummaryDto(9, 5, 3, 2, 1, LocalDateTime.now());
            
            when(rentalService.confirmRental(rentalId)).thenReturn(confirmedRentalResponse);
            when(dashboardQueryService.fetchDailySummary()).thenReturn(updatedSummary);

            QuickActionResultDto result = quickActionService.approveRental(rentalId);

            assertThat(result.updatedSummary().pendingApprovals()).isEqualTo(9);
            assertThat(result.updatedSummary()).isNotEqualTo(initialSummary);
        }
    }
}
