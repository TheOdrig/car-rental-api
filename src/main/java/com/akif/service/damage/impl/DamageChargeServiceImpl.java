package com.akif.service.damage.impl;

import com.akif.shared.enums.DamageStatus;
import com.akif.shared.enums.PaymentStatus;
import com.akif.event.DamageChargedEvent;
import com.akif.exception.DamageAssessmentException;
import com.akif.model.DamageReport;
import com.akif.model.Payment;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.PaymentRepository;
import com.akif.service.damage.IDamageChargeService;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DamageChargeServiceImpl implements IDamageChargeService {

    private final DamageReportRepository damageReportRepository;
    private final PaymentRepository paymentRepository;
    private final IPaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Payment createDamageCharge(DamageReport damageReport) {
        if (!damageReport.canBeCharged()) {
            throw DamageAssessmentException.invalidStatus(damageReport.getStatus().name());
        }

        if (damageReport.getCustomerLiability() == null || 
                damageReport.getCustomerLiability().signum() <= 0) {
            throw new DamageAssessmentException("Customer liability must be positive to create charge");
        }

        Payment payment = Payment.builder()
                .rental(damageReport.getRental())
                .amount(damageReport.getCustomerLiability())
                .currency(damageReport.getRental().getCurrency())
                .status(PaymentStatus.PENDING)
                .paymentMethod("DAMAGE_CHARGE")
                .build();

        payment = paymentRepository.save(payment);

        damageReport.setPaymentId(payment.getId());
        damageReport.setPaymentStatus(PaymentStatus.PENDING.name());
        damageReportRepository.save(damageReport);

        log.info("Damage charge created: paymentId={}, damageId={}, amount={}",
                payment.getId(), damageReport.getId(), payment.getAmount());

        return payment;
    }

    @Override
    @Transactional
    public PaymentResult chargeDamage(Payment damagePayment) {
        DamageReport damageReport = damageReportRepository
                .findByPaymentId(damagePayment.getId())
                .orElseThrow(() -> new DamageAssessmentException("Damage report not found for payment"));

        PaymentResult result = paymentGateway.authorize(
                damagePayment.getAmount(),
                damagePayment.getCurrency(),
                damageReport.getRental().getUser().getId().toString()
        );

        if (result.success()) {
            damagePayment.updateStatus(PaymentStatus.CAPTURED);
            damagePayment.setTransactionId(result.transactionId());
            paymentRepository.save(damagePayment);

            damageReport.updateStatus(DamageStatus.CHARGED);
            damageReport.setPaymentStatus(PaymentStatus.CAPTURED.name());
            damageReportRepository.save(damageReport);

            eventPublisher.publishEvent(new DamageChargedEvent(this, damageReport, damagePayment));
            log.info("Damage charged successfully: damageId={}, transactionId={}",
                    damageReport.getId(), result.transactionId());
        } else {
            handleFailedDamageCharge(damagePayment);
        }

        return result;
    }

    @Override
    @Transactional
    public void handleFailedDamageCharge(Payment damagePayment) {
        damagePayment.updateStatus(PaymentStatus.PENDING);
        damagePayment.setFailureReason("Payment authorization failed");
        paymentRepository.save(damagePayment);

        log.warn("Damage charge failed: paymentId={}, admin notification required",
                damagePayment.getId());

        // TODO: Publish AdminNotificationEvent for failed charge
    }
}
