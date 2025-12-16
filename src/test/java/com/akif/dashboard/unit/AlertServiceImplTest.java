package com.akif.dashboard.unit;

import com.akif.car.api.CarService;
import com.akif.car.api.CarSummaryResponse;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.damage.api.DamageService;
import com.akif.dashboard.api.dto.AlertDto;
import com.akif.dashboard.domain.enums.AlertSeverity;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.exception.AlertNotFoundException;
import com.akif.dashboard.internal.mapper.DashboardMapper;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.dashboard.internal.service.AlertServiceImpl;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.internal.dto.response.UserSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertServiceImpl Unit Tests")
class AlertServiceImplTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private RentalService rentalService;

    @Mock
    private CarService carService;

    @Mock
    private DamageService damageService;

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private AlertServiceImpl alertService;

    @Captor
    private ArgumentCaptor<Alert> alertCaptor;

    private Alert testAlert;
    private AlertDto testAlertDto;

    @BeforeEach
    void setUp() {
        testAlert = Alert.builder()
                .id(1L)
                .type(AlertType.LATE_RETURN)
                .severity(AlertSeverity.CRITICAL)
                .title("Critical Late Return")
                .message("Rental #1 is 48 hours overdue")
                .actionUrl("/admin/rentals/1")
                .referenceId(1L)
                .acknowledged(false)
                .build();

        testAlertDto = new AlertDto(
                1L,
                AlertType.LATE_RETURN,
                AlertSeverity.CRITICAL,
                "Critical Late Return",
                "Rental #1 is 48 hours overdue",
                "/admin/rentals/1",
                false,
                null,
                null,
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Generate Alerts Operations")
    class GenerateAlertsOperations {

        @Test
        @DisplayName("Should generate CRITICAL alert for late return >24 hours")
        void shouldGenerateCriticalAlertForLateReturn() {
            LocalDate overdueEndDate = LocalDate.now().minusDays(2);
            RentalResponse overdueRental = createOverdueRental(1L, overdueEndDate);
            Page<RentalResponse> overdueRentalsPage = new PageImpl<>(List.of(overdueRental));

            when(rentalService.findOverdueRentals(any(Pageable.class))).thenReturn(overdueRentalsPage);
            when(alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.LATE_RETURN, 1L)).thenReturn(false);
            when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

            when(carService.countTotalActiveCars()).thenReturn(100);
            when(carService.countByStatus(CarStatusType.AVAILABLE)).thenReturn(50);

            when(damageService.countUnresolvedDisputesOlderThan(7)).thenReturn(0);
            when(paymentService.countFailedPayments()).thenReturn(0);

            alertService.generateAlerts();

            verify(alertRepository).save(alertCaptor.capture());
            Alert savedAlert = alertCaptor.getValue();
            assertThat(savedAlert.getType()).isEqualTo(AlertType.LATE_RETURN);
            assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
            assertThat(savedAlert.getTitle()).isEqualTo("Critical Late Return");
        }

        @Test
        @DisplayName("Should not generate duplicate late return alert for same rental")
        void shouldNotGenerateDuplicateLateReturnAlert() {
            LocalDate overdueEndDate = LocalDate.now().minusDays(2);
            RentalResponse overdueRental = createOverdueRental(1L, overdueEndDate);
            Page<RentalResponse> overdueRentalsPage = new PageImpl<>(List.of(overdueRental));

            when(rentalService.findOverdueRentals(any(Pageable.class))).thenReturn(overdueRentalsPage);
            when(alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.LATE_RETURN, 1L)).thenReturn(true);

            when(carService.countTotalActiveCars()).thenReturn(100);
            when(carService.countByStatus(CarStatusType.AVAILABLE)).thenReturn(50);
            when(damageService.countUnresolvedDisputesOlderThan(7)).thenReturn(0);
            when(paymentService.countFailedPayments()).thenReturn(0);

            alertService.generateAlerts();

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Should generate WARNING alert for low availability <20%")
        void shouldGenerateWarningAlertForLowAvailability() {
            when(rentalService.findOverdueRentals(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            when(carService.countTotalActiveCars()).thenReturn(100);
            when(carService.countByStatus(CarStatusType.AVAILABLE)).thenReturn(15);
            when(alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.LOW_AVAILABILITY, 0L)).thenReturn(false);
            when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

            when(damageService.countUnresolvedDisputesOlderThan(7)).thenReturn(0);
            when(paymentService.countFailedPayments()).thenReturn(0);

            alertService.generateAlerts();

            verify(alertRepository).save(alertCaptor.capture());
            Alert savedAlert = alertCaptor.getValue();
            assertThat(savedAlert.getType()).isEqualTo(AlertType.LOW_AVAILABILITY);
            assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
            assertThat(savedAlert.getTitle()).isEqualTo("Low Fleet Availability");
        }

        @Test
        @DisplayName("Should not generate low availability alert when availability is above threshold")
        void shouldNotGenerateLowAvailabilityAlertWhenAboveThreshold() {
            when(rentalService.findOverdueRentals(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            when(carService.countTotalActiveCars()).thenReturn(100);
            when(carService.countByStatus(CarStatusType.AVAILABLE)).thenReturn(25);

            when(damageService.countUnresolvedDisputesOlderThan(7)).thenReturn(0);
            when(paymentService.countFailedPayments()).thenReturn(0);

            alertService.generateAlerts();

            verify(alertRepository, never()).save(any(Alert.class));
        }

        @Test
        @DisplayName("Should generate MEDIUM alert for unresolved disputes >7 days")
        void shouldGenerateMediumAlertForUnresolvedDisputes() {
            when(rentalService.findOverdueRentals(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            when(carService.countTotalActiveCars()).thenReturn(100);
            when(carService.countByStatus(CarStatusType.AVAILABLE)).thenReturn(50);

            when(damageService.countUnresolvedDisputesOlderThan(7)).thenReturn(3);
            when(alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.UNRESOLVED_DISPUTE, 0L)).thenReturn(false);
            when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);
            when(paymentService.countFailedPayments()).thenReturn(0);

            alertService.generateAlerts();

            verify(alertRepository).save(alertCaptor.capture());
            Alert savedAlert = alertCaptor.getValue();
            assertThat(savedAlert.getType()).isEqualTo(AlertType.UNRESOLVED_DISPUTE);
            assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
            assertThat(savedAlert.getTitle()).isEqualTo("Unresolved Damage Disputes");
        }
    }

    @Nested
    @DisplayName("Acknowledge Alert Operations")
    class AcknowledgeAlertOperations {

        @Test
        @DisplayName("Should acknowledge alert successfully")
        void shouldAcknowledgeAlertSuccessfully() {
            when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
            when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(dashboardMapper.toAlertDto(any(Alert.class))).thenReturn(testAlertDto);

            AlertDto result = alertService.acknowledgeAlert(1L, "admin");

            verify(alertRepository).save(alertCaptor.capture());
            Alert savedAlert = alertCaptor.getValue();
            assertThat(savedAlert.getAcknowledged()).isTrue();
            assertThat(savedAlert.getAcknowledgedBy()).isEqualTo("admin");
            assertThat(savedAlert.getAcknowledgedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should return existing alert when already acknowledged")
        void shouldReturnExistingAlertWhenAlreadyAcknowledged() {
            testAlert.acknowledge("previousAdmin");
            when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
            when(dashboardMapper.toAlertDto(testAlert)).thenReturn(testAlertDto);

            AlertDto result = alertService.acknowledgeAlert(1L, "newAdmin");

            verify(alertRepository, never()).save(any(Alert.class));
            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("Should throw AlertNotFoundException when alert not found")
        void shouldThrowWhenAlertNotFound() {
            when(alertRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> alertService.acknowledgeAlert(999L, "admin"))
                    .isInstanceOf(AlertNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Alerts Operations")
    class GetAlertsOperations {

        @Test
        @DisplayName("Should get all active alerts sorted by severity")
        void shouldGetActiveAlertsSortedBySeverity() {
            List<Alert> activeAlerts = List.of(testAlert);
            List<AlertDto> expectedDtos = List.of(testAlertDto);

            when(alertRepository.findByAcknowledgedFalseOrderBySeverityAsc()).thenReturn(activeAlerts);
            when(dashboardMapper.toAlertDtoList(activeAlerts)).thenReturn(expectedDtos);

            List<AlertDto> result = alertService.getActiveAlerts();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).severity()).isEqualTo(AlertSeverity.CRITICAL);
        }

        @Test
        @DisplayName("Should get alerts by type")
        void shouldGetAlertsByType() {
            List<Alert> lateReturnAlerts = List.of(testAlert);
            List<AlertDto> expectedDtos = List.of(testAlertDto);

            when(alertRepository.findByTypeAndAcknowledgedFalse(AlertType.LATE_RETURN))
                    .thenReturn(lateReturnAlerts);
            when(dashboardMapper.toAlertDtoList(lateReturnAlerts)).thenReturn(expectedDtos);

            List<AlertDto> result = alertService.getAlertsByType(AlertType.LATE_RETURN);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).type()).isEqualTo(AlertType.LATE_RETURN);
        }

        @Test
        @DisplayName("Should return empty list when no active alerts")
        void shouldReturnEmptyListWhenNoActiveAlerts() {
            when(alertRepository.findByAcknowledgedFalseOrderBySeverityAsc())
                    .thenReturn(Collections.emptyList());
            when(dashboardMapper.toAlertDtoList(Collections.emptyList()))
                    .thenReturn(Collections.emptyList());

            List<AlertDto> result = alertService.getActiveAlerts();

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Alert Severity Ordering")
    class AlertSeverityOrdering {

        @Test
        @DisplayName("Should verify CRITICAL has highest priority (1)")
        void shouldVerifyCriticalHasHighestPriority() {
            assertThat(AlertSeverity.CRITICAL.getPriority()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should verify severity priority ordering")
        void shouldVerifySeverityPriorityOrdering() {
            assertThat(AlertSeverity.CRITICAL.getPriority()).isLessThan(AlertSeverity.HIGH.getPriority());
            assertThat(AlertSeverity.HIGH.getPriority()).isLessThan(AlertSeverity.WARNING.getPriority());
            assertThat(AlertSeverity.WARNING.getPriority()).isLessThan(AlertSeverity.MEDIUM.getPriority());
            assertThat(AlertSeverity.MEDIUM.getPriority()).isLessThan(AlertSeverity.LOW.getPriority());
        }
    }
    

    private RentalResponse createOverdueRental(Long id, LocalDate endDate) {
        UserSummaryResponse userSummary = new UserSummaryResponse(1L, "testuser", "test@example.com");
        CarSummaryResponse carSummary = new CarSummaryResponse(
                1L, "34ABC123", "Toyota", "Corolla", 2023,
                "1000 TL", null, CarStatusType.RESERVED, "White", 50000L,
                null, false, null, 100L, 50L, 2, "Toyota Corolla", "Toyota Corolla 2023", false, false
        );

        return new RentalResponse(
                id, carSummary, userSummary,
                LocalDate.now().minusDays(5), endDate,
                5, null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null
        );
    }
}
