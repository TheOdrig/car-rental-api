package com.akif.damage.internal.service.damage.impl;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarService;
import com.akif.damage.internal.config.DamageConfig;
import com.akif.damage.api.DamageAssessedEvent;
import com.akif.damage.domain.enums.DamageSeverity;
import com.akif.damage.domain.enums.DamageStatus;
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
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageAssessmentServiceImpl implements DamageAssessmentService {

    private final DamageReportRepository damageReportRepository;
    private final DamageConfig damageConfig;
    private final ApplicationEventPublisher eventPublisher;

    private final CarService carService;
    private final AuthService authService;

    @Override
    @Transactional
    public DamageAssessmentResponse assessDamage(Long damageId, DamageAssessmentRequest request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = authService.getUserByUsername(username);

        if (!damageReport.canBeAssessed()) {
            throw DamageAssessmentException.invalidStatus(damageReport.getStatus().name());
        }

        DamageSeverity severity = request.severity() != null
                ? request.severity()
                : determineSeverity(request.repairCostEstimate());

        boolean hasInsurance = Boolean.TRUE.equals(request.insuranceCoverage());
        BigDecimal deductible = request.insuranceDeductible() != null
                ? request.insuranceDeductible()
                : damageConfig.getDefaultInsuranceDeductible();

        BigDecimal customerLiability = calculateCustomerLiability(
                request.repairCostEstimate(),
                hasInsurance,
                deductible
        );

        damageReport.setSeverity(severity);
        damageReport.setCategory(request.category());
        damageReport.setRepairCostEstimate(request.repairCostEstimate());
        damageReport.setCustomerLiability(customerLiability);
        damageReport.setInsuranceCoverage(hasInsurance);
        damageReport.setInsuranceDeductible(deductible);
        damageReport.setAssessmentNotes(request.assessmentNotes());
        damageReport.setAssessedBy(user.id());
        damageReport.setAssessedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.ASSESSED);

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCarId(), severity);

        eventPublisher.publishEvent(new DamageAssessedEvent(
                this,
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
    public DamageAssessmentResponse updateAssessment(Long damageId, DamageAssessmentRequest request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        if (damageReport.getStatus() != DamageStatus.ASSESSED) {
            throw DamageAssessmentException.alreadyCharged();
        }

        UserDto user = authService.getUserByUsername(username);

        DamageSeverity severity = request.severity() != null
                ? request.severity()
                : determineSeverity(request.repairCostEstimate());

        boolean hasInsurance = Boolean.TRUE.equals(request.insuranceCoverage());
        BigDecimal deductible = request.insuranceDeductible() != null
                ? request.insuranceDeductible()
                : damageConfig.getDefaultInsuranceDeductible();

        BigDecimal customerLiability = calculateCustomerLiability(
                request.repairCostEstimate(),
                hasInsurance,
                deductible
        );

        damageReport.setSeverity(severity);
        damageReport.setCategory(request.category());
        damageReport.setRepairCostEstimate(request.repairCostEstimate());
        damageReport.setCustomerLiability(customerLiability);
        damageReport.setInsuranceCoverage(hasInsurance);
        damageReport.setInsuranceDeductible(deductible);
        damageReport.setAssessmentNotes(request.assessmentNotes());
        damageReport.setAssessedBy(user.id());
        damageReport.setAssessedAt(LocalDateTime.now());

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCarId(), severity);

        log.info("Assessment updated: id={}, severity={}, liability={}",
                damageId, severity, customerLiability);

        return buildResponseDto(damageReport, carStatusUpdated);
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
            carService.markAsMaintenance(carId);
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
                damageReport.getInsuranceCoverage(),
                damageReport.getInsuranceDeductible(),
                damageReport.getAssessmentNotes(),
                damageReport.getAssessedAt(),
                damageReport.getAssessedBy(),
                carStatusUpdated
        );
    }
}
