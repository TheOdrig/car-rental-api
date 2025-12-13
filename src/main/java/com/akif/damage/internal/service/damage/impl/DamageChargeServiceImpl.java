package com.akif.damage.internal.service.damage.impl;

import com.akif.damage.api.DamageChargedEvent;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.service.damage.DamageChargeService;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.exception.DamageAssessmentException;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.payment.api.PaymentService;
import com.akif.payment.api.PaymentResult;
import com.akif.shared.enums.CurrencyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageChargeServiceImpl implements DamageChargeService {

    private final DamageReportRepository damageReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final RentalService rentalService;
    private final PaymentService paymentService;

    @Override
    @Transactional
    public Object createDamageCharge(DamageReport damageReport) {
        if (!damageReport.canBeCharged()) {
            throw DamageAssessmentException.invalidStatus(damageReport.getStatus().name());
        }

        if (damageReport.getCustomerLiability() == null || 
                damageReport.getCustomerLiability().signum() <= 0) {
            throw new DamageAssessmentException("Customer liability must be positive to create charge");
        }

        RentalSummaryDto rental = rentalService.getRentalSummaryById(damageReport.getRentalId());

        log.info("Creating damage charge: damageId={}, amount={}, userId={}", 
                damageReport.getId(), damageReport.getCustomerLiability(), rental.userId());

        damageReport.setPaymentStatus("PENDING");
        damageReportRepository.save(damageReport);

        return null;
    }

    @Override
    @Transactional
    public PaymentResult chargeDamage(Object damagePayment) {
        if (damagePayment instanceof Long damageId) {
            return chargeDamageById(damageId);
        }
        
        log.warn("chargeDamage() called with invalid type. Use chargeDamageById(Long) instead.");
        return new PaymentResult(false, null, "Use chargeDamageById(Long damageId) instead");
    }

    @Transactional
    public PaymentResult chargeDamageById(Long damageId) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> new DamageAssessmentException("Damage report not found: " + damageId));

        RentalSummaryDto rental = rentalService.getRentalSummaryById(damageReport.getRentalId());

        PaymentResult result = paymentService.authorize(
                damageReport.getCustomerLiability(),
                CurrencyType.TRY,
                rental.userId().toString()
        );

        if (result.success()) {
            damageReport.updateStatus(DamageStatus.CHARGED);
            damageReport.setPaymentStatus("CAPTURED");
            damageReport.setTransactionId(result.transactionId());
            damageReportRepository.save(damageReport);

            eventPublisher.publishEvent(new DamageChargedEvent(
                    this,
                    damageReport.getId(),
                    damageReport.getRentalId(),
                    damageReport.getCustomerEmail(),
                    damageReport.getCustomerLiability(),
                    result.transactionId(),
                    LocalDateTime.now()
            ));
            log.info("Damage charged successfully: damageId={}, transactionId={}",
                    damageReport.getId(), result.transactionId());
        } else {
            handleFailedDamageCharge(damageReport);
        }

        return result;
    }

    @Override
    @Transactional
    public void handleFailedDamageCharge(Object damagePayment) {
        if (damagePayment instanceof DamageReport damageReport) {
            damageReport.setPaymentStatus("FAILED");
            damageReportRepository.save(damageReport);
            log.warn("Damage charge failed: damageId={}, admin notification required", damageReport.getId());
        } else if (damagePayment instanceof Long damageId) {
            DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                    .orElse(null);
            if (damageReport != null) {
                damageReport.setPaymentStatus("FAILED");
                damageReportRepository.save(damageReport);
                log.warn("Damage charge failed: damageId={}, admin notification required", damageId);
            }
        }
    }

    private void handleFailedDamageCharge(DamageReport damageReport) {
        damageReport.setPaymentStatus("FAILED");
        damageReportRepository.save(damageReport);
        log.warn("Damage charge failed: damageId={}, admin notification required", damageReport.getId());
    }
}

