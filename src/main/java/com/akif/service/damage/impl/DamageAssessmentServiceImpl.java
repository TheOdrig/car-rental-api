package com.akif.service.damage.impl;

import com.akif.config.DamageConfig;
import com.akif.dto.damage.request.DamageAssessmentRequestDto;
import com.akif.dto.damage.response.DamageAssessmentResponseDto;
import com.akif.shared.enums.DamageSeverity;
import com.akif.shared.enums.DamageStatus;
import com.akif.event.DamageAssessedEvent;
import com.akif.exception.DamageAssessmentException;
import com.akif.exception.DamageReportException;
import com.akif.model.Car;
import com.akif.model.DamageReport;
import com.akif.repository.CarRepository;
import com.akif.repository.DamageReportRepository;
import com.akif.auth.repository.UserRepository;
import com.akif.auth.domain.User;
import com.akif.service.damage.IDamageAssessmentService;
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
public class DamageAssessmentServiceImpl implements IDamageAssessmentService {

    private final DamageReportRepository damageReportRepository;
    private final CarRepository carRepository;
    private final UserRepository userRepository;
    private final DamageConfig damageConfig;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DamageAssessmentResponseDto assessDamage(Long damageId, DamageAssessmentRequestDto request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

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
        damageReport.setAssessedBy(user.getId());
        damageReport.setAssessedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.ASSESSED);

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCar(), severity);

        eventPublisher.publishEvent(new DamageAssessedEvent(this, damageReport));
        log.info("Damage assessed: id={}, severity={}, liability={}",
                damageId, severity, customerLiability);

        return buildResponseDto(damageReport, carStatusUpdated);
    }

    @Override
    @Transactional
    public DamageAssessmentResponseDto updateAssessment(Long damageId, DamageAssessmentRequestDto request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        if (damageReport.getStatus() != DamageStatus.ASSESSED) {
            throw DamageAssessmentException.alreadyCharged();
        }

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

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
        damageReport.setAssessedBy(user.getId());
        damageReport.setAssessedAt(LocalDateTime.now());

        damageReport = damageReportRepository.save(damageReport);

        String carStatusUpdated = updateCarStatusBasedOnSeverity(damageReport.getCar(), severity);

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

    private String updateCarStatusBasedOnSeverity(Car car, DamageSeverity severity) {
        if (severity.requiresMaintenance()) {
            car.markAsMaintenance();
            carRepository.save(car);
            log.info("Car {} marked as MAINTENANCE due to {} damage",
                    car.getLicensePlate(), severity);
            return "MAINTENANCE";
        }

        if (severity.requiresAdminDecision()) {
            log.info("Car {} requires admin decision for MODERATE damage", car.getLicensePlate());
            return "ADMIN_DECISION_REQUIRED";
        }

        return null;
    }

    private DamageAssessmentResponseDto buildResponseDto(DamageReport damageReport, String carStatusUpdated) {
        return new DamageAssessmentResponseDto(
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
