package com.akif.damage.internal.service.damage.impl;

import java.time.Clock;

import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarFleetOperations;
import com.akif.rental.api.RentalService;
import com.akif.shared.enums.CurrencyType;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.api.DamageAssessedEvent;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.service.damage.DamageAssessmentService;
import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.response.DamageAssessmentResponse;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.exception.DamageAssessmentException;
import com.akif.damage.internal.exception.DamageReportException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageAssessmentServiceImpl implements DamageAssessmentService {

    private final Clock clock;

    private final DamageReportRepository damageReportRepository;
    private final DamageConfig damageConfig;
    private final ApplicationEventPublisher eventPublisher;

    private final CarFleetOperations carFleetOperations;
    private final UserLookup userLookup;
    private final RentalService rentalService;

    @Override
    @Transactional
    public DamageAssessmentResponse assessDamage(Long damageId, DamageAssessmentRequest request, Long userId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = userLookup.getUserById(userId);

        if (!damageReport.canBeAssessed()) {
            throw DamageAssessmentException.invalidStatus(damageReport.getStatus().name());
        }

        DamageSeverity severity = request.severity() != null
                ? request.severity()
                : determineSeverity(request.repairCostEstimate());

        CurrencyType currency = resolveCurrency(damageReport, request);
        int places = currency.getDecimalPlaces();
        boolean hasInsurance = Boolean.TRUE.equals(request.insuranceCoverage());
        BigDecimal deductible = round(request.insuranceDeductible() != null
                ? request.insuranceDeductible()
                : damageConfig.getDefaultInsuranceDeductible(), places);
        BigDecimal repairCost = round(request.repairCostEstimate(), places);

        BigDecimal customerLiability = round(calculateCustomerLiability(repairCost, hasInsurance, deductible), places);

        damageReport.assess(severity, request.category(), repairCost, customerLiability,
                hasInsurance, deductible, request.assessmentNotes(), user.id(), Instant.now(clock));
        damageReport.assignCurrency(currency);

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCarId(), severity);

        eventPublisher.publishEvent(new DamageAssessedEvent(
                damageReport.getId(),
                damageReport.getRentalId(),
                damageReport.getCarId(),
                damageReport.getCarLicensePlate(),
                damageReport.getCustomerEmail(),
                damageReport.getSeverity(),
                damageReport.getRepairCostEstimate(),
                damageReport.getCustomerLiability(),
                damageReport.getAssessedAt()
        ));
        log.info("Damage assessed: id={}, severity={}, liability={}",
                damageId, severity, customerLiability);

        return buildResponseDto(damageReport, carStatusUpdated);
    }

    @Override
    @Transactional
    public DamageAssessmentResponse updateAssessment(Long damageId, DamageAssessmentRequest request, Long userId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        if (damageReport.getStatus() != DamageStatus.ASSESSED) {
            throw DamageAssessmentException.alreadyCharged();
        }

        UserDto user = userLookup.getUserById(userId);

        DamageSeverity severity = request.severity() != null
                ? request.severity()
                : determineSeverity(request.repairCostEstimate());

        CurrencyType currency = resolveCurrency(damageReport, request);
        int places = currency.getDecimalPlaces();
        boolean hasInsurance = Boolean.TRUE.equals(request.insuranceCoverage());
        BigDecimal deductible = round(request.insuranceDeductible() != null
                ? request.insuranceDeductible()
                : damageConfig.getDefaultInsuranceDeductible(), places);
        BigDecimal repairCost = round(request.repairCostEstimate(), places);

        BigDecimal customerLiability = round(calculateCustomerLiability(repairCost, hasInsurance, deductible), places);

        damageReport.reviseAssessment(severity, request.category(), repairCost, customerLiability,
                hasInsurance, deductible, request.assessmentNotes(), user.id(), Instant.now(clock));
        damageReport.assignCurrency(currency);

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCarId(), severity);

        log.info("Assessment updated: id={}, severity={}, liability={}",
                damageId, severity, customerLiability);

        return buildResponseDto(damageReport, carStatusUpdated);
    }

    private static BigDecimal round(BigDecimal amount, int places) {
        return amount == null ? null : amount.setScale(places, java.math.RoundingMode.HALF_UP);
    }

    private CurrencyType resolveCurrency(DamageReport damageReport, DamageAssessmentRequest request) {
        CurrencyType rentalCurrency = rentalService.getRentalSummaryById(damageReport.getRentalId()).currency();
        if (request.currency() != null && request.currency() != rentalCurrency) {
            throw new DamageAssessmentException(
                    "Damage currency must match the rental currency (" + rentalCurrency + ")");
        }
        return rentalCurrency;
    }

    @Override
    public BigDecimal calculateCustomerLiability(BigDecimal repairCost, boolean hasInsurance, BigDecimal deductible) {
        if (repairCost == null || repairCost.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        if (hasInsurance && deductible != null) {
            return repairCost.min(deductible);
        }

        return repairCost;
    }

    @Override
    public DamageSeverity determineSeverity(BigDecimal repairCost) {
        if (repairCost == null) {
            return DamageSeverity.MINOR;
        }

        if (repairCost.compareTo(damageConfig.getMinorThreshold()) < 0) {
            return DamageSeverity.MINOR;
        }
        if (repairCost.compareTo(damageConfig.getModerateThreshold()) < 0) {
            return DamageSeverity.MODERATE;
        }
        if (repairCost.compareTo(damageConfig.getMajorThreshold()) < 0) {
            return DamageSeverity.MAJOR;
        }
        return DamageSeverity.TOTAL_LOSS;
    }

    private String updateCarStatusBasedOnSeverity(Long carId, DamageSeverity severity) {
        if (severity.requiresMaintenance()) {
            carFleetOperations.markAsMaintenance(carId);
            log.info("Car {} marked as MAINTENANCE due to {} damage", carId, severity);
            return "MAINTENANCE";
        }

        if (severity.requiresAdminDecision()) {
            log.info("Car {} requires admin decision for MODERATE damage", carId);
            return "ADMIN_DECISION_REQUIRED";
        }

        return null;
    }

    private DamageAssessmentResponse buildResponseDto(DamageReport damageReport, String carStatusUpdated) {
        return new DamageAssessmentResponse(
                damageReport.getId(),
                damageReport.getSeverity(),
                damageReport.getRepairCostEstimate(),
                damageReport.getCustomerLiability(),
                damageReport.getCurrency(),
                damageReport.getInsuranceCoverage(),
                damageReport.getInsuranceDeductible(),
                damageReport.getAssessmentNotes(),
                damageReport.getAssessedAt(),
                damageReport.getAssessedBy(),
                carStatusUpdated
        );
    }
}
