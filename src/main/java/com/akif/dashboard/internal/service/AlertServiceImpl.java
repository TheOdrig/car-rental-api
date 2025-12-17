package com.akif.dashboard.internal.service;

import com.akif.car.api.CarService;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.damage.api.DamageService;
import com.akif.dashboard.api.AlertService;
import com.akif.dashboard.api.dto.AlertDto;
import com.akif.dashboard.domain.enums.AlertSeverity;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.mapper.DashboardMapper;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.payment.api.PaymentService;
import com.akif.dashboard.internal.exception.AlertNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AlertServiceImpl implements AlertService {

    private static final BigDecimal LOW_AVAILABILITY_THRESHOLD = new BigDecimal("20.00");
    private static final int DISPUTE_DAYS_THRESHOLD = 7;
    private static final int LATE_HOURS_CRITICAL_THRESHOLD = 24;

    private final AlertRepository alertRepository;
    private final DashboardMapper dashboardMapper;
    private final RentalService rentalService;
    private final CarService carService;
    private final DamageService damageService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    @Scheduled(fixedRate = 300000)
    public void generateAlerts() {
        log.info("Starting scheduled alert generation");

        generateLateReturnAlerts();
        generateLowAvailabilityAlert();
        generateUnresolvedDisputeAlerts();
        generateFailedPaymentAlerts();

        log.info("Completed scheduled alert generation");
    }

    @Override
    @Transactional
    public AlertDto acknowledgeAlert(Long alertId, String adminUsername) {
        log.info("Acknowledging alert {} by admin {}", alertId, adminUsername);

        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));

        if (alert.isAcknowledged()) {
            log.warn("Alert {} is already acknowledged", alertId);
            return dashboardMapper.toAlertDto(alert);
        }

        alert.acknowledge(adminUsername);
        Alert savedAlert = alertRepository.save(alert);

        log.info("Alert {} acknowledged successfully", alertId);
        return dashboardMapper.toAlertDto(savedAlert);
    }

    @Override
    public List<AlertDto> getActiveAlerts() {
        log.debug("Fetching all active (unacknowledged) alerts");
        List<Alert> alerts = alertRepository.findByAcknowledgedFalseOrderBySeverityAsc();
        return dashboardMapper.toAlertDtoList(alerts);
    }

    @Override
    public List<AlertDto> getAlertsByType(AlertType type) {
        log.debug("Fetching active alerts by type: {}", type);
        List<Alert> alerts = alertRepository.findByTypeAndAcknowledgedFalse(type);
        return dashboardMapper.toAlertDtoList(alerts);
    }

    private void generateLateReturnAlerts() {
        log.debug("Checking for late return alerts (>24 hours)");

        Pageable pageable = PageRequest.of(0, 100);
        List<RentalResponse> overdueRentals = rentalService.findOverdueRentals(pageable).getContent();

        for (RentalResponse rental : overdueRentals) {
            long hoursLate = calculateHoursLate(rental.endDate());

            if (hoursLate >= LATE_HOURS_CRITICAL_THRESHOLD) {
                boolean alertExists = alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                        AlertType.LATE_RETURN, rental.id());

                if (!alertExists) {
                    createLateReturnAlert(rental, hoursLate);
                }
            }
        }
    }

    private void generateLowAvailabilityAlert() {
        log.debug("Checking for low availability alert (<20%)");

        int totalCars = carService.countTotalActiveCars();
        int availableCars = carService.countByStatus(CarStatusType.AVAILABLE);

        if (totalCars == 0) {
            log.warn("No active cars in the system, skipping low availability check");
            return;
        }

        BigDecimal availabilityRate = BigDecimal.valueOf(availableCars)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalCars), 2, RoundingMode.HALF_UP);

        if (availabilityRate.compareTo(LOW_AVAILABILITY_THRESHOLD) < 0) {
            boolean alertExists = alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.LOW_AVAILABILITY, 0L);

            if (!alertExists) {
                createLowAvailabilityAlert(availabilityRate, availableCars, totalCars);
            }
        }
    }

    private void generateUnresolvedDisputeAlerts() {
        log.debug("Checking for unresolved dispute alerts (>7 days)");

        int unresolvedCount = damageService.countUnresolvedDisputesOlderThan(DISPUTE_DAYS_THRESHOLD);

        if (unresolvedCount > 0) {
            boolean alertExists = alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.UNRESOLVED_DISPUTE, 0L);

            if (!alertExists) {
                createUnresolvedDisputeAlert(unresolvedCount);
            }
        }
    }

    private void generateFailedPaymentAlerts() {
        log.debug("Checking for failed payment alerts");

        int failedCount = paymentService.countFailedPayments();

        if (failedCount > 0) {
            boolean alertExists = alertRepository.existsByTypeAndReferenceIdAndAcknowledgedFalse(
                    AlertType.FAILED_PAYMENT, 0L);

            if (!alertExists) {
                createFailedPaymentAlert(failedCount);
            }
        }
    }

    @Transactional
    protected void createLateReturnAlert(RentalResponse rental, long hoursLate) {
        String customerName = rental.userSummary() != null 
                ? rental.userSummary().username() 
                : "Unknown";
        String carInfo = rental.carSummary() != null
                ? String.format("%s %s (%s)", 
                        rental.carSummary().brand(), 
                        rental.carSummary().model(),
                        rental.carSummary().licensePlate())
                : "Unknown Car";

        Alert alert = Alert.builder()
                .type(AlertType.LATE_RETURN)
                .severity(AlertSeverity.CRITICAL)
                .title("Critical Late Return")
                .message(String.format(
                        "Rental #%d is %d hours overdue. Customer: %s, Car: %s",
                        rental.id(), hoursLate, customerName, carInfo))
                .actionUrl("/admin/rentals/" + rental.id())
                .referenceId(rental.id())
                .acknowledged(false)
                .build();

        alertRepository.save(alert);
        log.info("Created CRITICAL late return alert for rental {}", rental.id());
    }

    @Transactional
    protected void createLowAvailabilityAlert(BigDecimal rate, int available, int total) {
        Alert alert = Alert.builder()
                .type(AlertType.LOW_AVAILABILITY)
                .severity(AlertSeverity.WARNING)
                .title("Low Fleet Availability")
                .message(String.format(
                        "Fleet availability is at %s%% (%d of %d cars available). Consider reviewing maintenance schedules.",
                        rate.setScale(1, RoundingMode.HALF_UP), available, total))
                .actionUrl("/admin/dashboard/fleet")
                .referenceId(0L)
                .acknowledged(false)
                .build();

        alertRepository.save(alert);
        log.info("Created WARNING low availability alert: {}% availability", rate);
    }

    @Transactional
    protected void createUnresolvedDisputeAlert(int count) {
        Alert alert = Alert.builder()
                .type(AlertType.UNRESOLVED_DISPUTE)
                .severity(AlertSeverity.MEDIUM)
                .title("Unresolved Damage Disputes")
                .message(String.format(
                        "%d damage dispute(s) have been unresolved for more than %d days. Please review and take action.",
                        count, DISPUTE_DAYS_THRESHOLD))
                .actionUrl("/admin/damage/disputes")
                .referenceId(0L)
                .acknowledged(false)
                .build();

        alertRepository.save(alert);
        log.info("Created MEDIUM unresolved dispute alert: {} disputes", count);
    }

    @Transactional
    protected void createFailedPaymentAlert(int count) {
        Alert alert = Alert.builder()
                .type(AlertType.FAILED_PAYMENT)
                .severity(AlertSeverity.HIGH)
                .title("Failed Payment Attempts")
                .message(String.format(
                        "%d payment(s) have failed. Review and retry or contact customers.",
                        count))
                .actionUrl("/admin/payments?status=FAILED")
                .referenceId(0L)
                .acknowledged(false)
                .build();

        alertRepository.save(alert);
        log.info("Created HIGH failed payment alert: {} failures", count);
    }

    private long calculateHoursLate(LocalDate endDate) {
        if (endDate == null) {
            return 0;
        }
        LocalDate today = LocalDate.now();
        if (!today.isAfter(endDate)) {
            return 0;
        }
        long daysLate = ChronoUnit.DAYS.between(endDate, today);
        return daysLate * 24;
    }
}
