package com.akif.service.damage.impl;

import com.akif.dto.damage.request.DamageDisputeRequestDto;
import com.akif.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.dto.damage.response.DamageDisputeResponseDto;
import com.akif.shared.enums.DamageStatus;
import com.akif.event.DamageDisputedEvent;
import com.akif.event.DamageResolvedEvent;
import com.akif.exception.DamageDisputeException;
import com.akif.exception.DamageReportException;
import com.akif.model.DamageReport;
import com.akif.model.Payment;
import com.akif.repository.DamageReportRepository;
import com.akif.repository.PaymentRepository;
import com.akif.auth.repository.UserRepository;
import com.akif.auth.domain.User;
import com.akif.service.damage.IDamageDisputeService;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
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
public class DamageDisputeServiceImpl implements IDamageDisputeService {

    private final DamageReportRepository damageReportRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final IPaymentGateway paymentGateway;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public DamageDisputeResponseDto createDispute(Long damageId, DamageDisputeRequestDto request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (!damageReport.canBeDisputed()) {
            throw DamageDisputeException.invalidStatus(damageReport.getStatus().name());
        }

        Long rentalOwnerId = damageReport.getRental().getUser().getId();
        if (!rentalOwnerId.equals(user.getId())) {
            log.warn("User {} attempted to dispute damage {} but is not the rental owner (ownerId={})",
                    user.getId(), damageId, rentalOwnerId);
            throw DamageDisputeException.notOwner();
        }

        damageReport.setDisputeReason(request.reason());
        damageReport.setDisputeComments(request.comments());
        damageReport.setDisputedBy(user.getId());
        damageReport.setDisputedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.DISPUTED);
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageDisputedEvent(this, damageReport));
        log.info("Dispute created: damageId={}, reason={}", damageId, request.reason());

        return buildDisputeResponse(damageReport, null);
    }

    @Override
    @Transactional
    public DamageDisputeResponseDto resolveDispute(Long damageId, DamageDisputeResolutionDto resolution, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        if (damageReport.getStatus() != DamageStatus.DISPUTED) {
            throw DamageDisputeException.cannotResolve(damageReport.getStatus().name());
        }

        BigDecimal originalLiability = damageReport.getCustomerLiability();
        BigDecimal adjustedLiability = resolution.adjustedCustomerLiability();
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (adjustedLiability.compareTo(originalLiability) < 0) {
            refundAmount = originalLiability.subtract(adjustedLiability);
            processRefundForAdjustment(damageReport, adjustedLiability);
        }

        damageReport.setCustomerLiability(adjustedLiability);
        damageReport.setRepairCostEstimate(resolution.adjustedRepairCost());
        damageReport.setResolutionNotes(resolution.resolutionNotes());
        damageReport.setResolvedBy(user.getId());
        damageReport.setResolvedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.RESOLVED);
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageResolvedEvent(this, damageReport, refundAmount));
        log.info("Dispute resolved: damageId={}, adjustedLiability={}, refundAmount={}",
                damageId, adjustedLiability, refundAmount);

        return buildDisputeResponse(damageReport, refundAmount);
    }

    @Override
    @Transactional
    public void processRefundForAdjustment(DamageReport damageReport, BigDecimal adjustedAmount) {
        if (damageReport.getPaymentId() == null) {
            log.warn("No payment found for damage report: {}", damageReport.getId());
            return;
        }

        Payment payment = paymentRepository.findById(damageReport.getPaymentId())
                .orElseThrow(() -> new DamageDisputeException("Payment not found for refund"));

        if (!payment.canRefund()) {
            throw new DamageDisputeException("Payment cannot be refunded in current status: " + payment.getStatus());
        }

        BigDecimal refundAmount = damageReport.getCustomerLiability().subtract(adjustedAmount);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        PaymentResult result = paymentGateway.refund(payment.getTransactionId(), refundAmount);

        if (result.success()) {
            BigDecimal totalRefunded = payment.getRefundedAmount() != null
                    ? payment.getRefundedAmount().add(refundAmount)
                    : refundAmount;
            payment.setRefundedAmount(totalRefunded);
            paymentRepository.save(payment);

            log.info("Refund processed: paymentId={}, refundAmount={}", payment.getId(), refundAmount);
        } else {
            throw new DamageDisputeException("Refund failed: " + result.message());
        }
    }

    private DamageDisputeResponseDto buildDisputeResponse(DamageReport damageReport, BigDecimal refundAmount) {
        return new DamageDisputeResponseDto(
                damageReport.getId(),
                damageReport.getStatus(),
                damageReport.getDisputeReason(),
                damageReport.getDisputeComments(),
                damageReport.getResolutionNotes(),
                damageReport.getCustomerLiability(),
                damageReport.getCustomerLiability(),
                refundAmount,
                damageReport.getDisputedAt(),
                damageReport.getResolvedAt()
        );
    }
}
