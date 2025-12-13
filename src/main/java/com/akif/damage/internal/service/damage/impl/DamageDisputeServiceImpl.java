package com.akif.damage.internal.service.damage.impl;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.damage.api.DamageDisputedEvent;
import com.akif.damage.api.DamageResolvedEvent;
import com.akif.damage.domain.enums.DamageStatus;
import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.service.damage.DamageDisputeService;
import com.akif.damage.internal.dto.damage.request.DamageDisputeRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.damage.internal.dto.damage.response.DamageDisputeResponse;
import com.akif.damage.internal.repository.DamageReportRepository;
import com.akif.damage.internal.exception.DamageDisputeException;
import com.akif.damage.internal.exception.DamageReportException;
import com.akif.rental.api.RentalService;
import com.akif.rental.api.RentalSummaryDto;
import com.akif.payment.api.PaymentService;
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
public class DamageDisputeServiceImpl implements DamageDisputeService {

    private final DamageReportRepository damageReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final AuthService authService;
    private final RentalService rentalService;

    private final PaymentService paymentService;

    @Override
    @Transactional
    public DamageDisputeResponse createDispute(Long damageId, DamageDisputeRequest request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = authService.getUserByUsername(username);

        if (!damageReport.canBeDisputed()) {
            throw DamageDisputeException.invalidStatus(damageReport.getStatus().name());
        }

        RentalSummaryDto rental = rentalService.getRentalSummaryById(damageReport.getRentalId());

        if (!rental.userId().equals(user.id())) {
            log.warn("User {} attempted to dispute damage {} but is not the rental owner (ownerId={})",
                    user.id(), damageId, rental.userId());
            throw DamageDisputeException.notOwner();
        }

        damageReport.setDisputeReason(request.reason());
        damageReport.setDisputeComments(request.comments());
        damageReport.setDisputedBy(user.id());
        damageReport.setDisputedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.DISPUTED);
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageDisputedEvent(
                this,
                damageReport.getId(),
                damageReport.getRentalId(),
                damageReport.getCustomerEmail(),
                damageReport.getDisputeReason(),
                damageReport.getDisputedAt()
        ));
        log.info("Dispute created: damageId={}, reason={}", damageId, request.reason());

        return buildDisputeResponse(damageReport, null);
    }

    @Override
    @Transactional
    public DamageDisputeResponse resolveDispute(Long damageId, DamageDisputeResolutionDto resolution, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = authService.getUserByUsername(username);

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
        damageReport.setResolvedBy(user.id());
        damageReport.setResolvedAt(LocalDateTime.now());
        damageReport.updateStatus(DamageStatus.RESOLVED);
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageResolvedEvent(
                this,
                damageReport.getId(),
                damageReport.getRentalId(),
                damageReport.getCustomerEmail(),
                damageReport.getCustomerLiability(),
                refundAmount,
                damageReport.getResolutionNotes(),
                damageReport.getResolvedAt()
        ));
        log.info("Dispute resolved: damageId={}, adjustedLiability={}, refundAmount={}",
                damageId, adjustedLiability, refundAmount);

        return buildDisputeResponse(damageReport, refundAmount);
    }

    @Override
    @Transactional
    public void processRefundForAdjustment(DamageReport damageReport, BigDecimal adjustedAmount) {
        String transactionId = damageReport.getTransactionId();
        
        if (transactionId == null || transactionId.isEmpty()) {
            log.warn("No transactionId found for damage report: {}. Cannot process refund.", damageReport.getId());
            return;
        }

        BigDecimal refundAmount = damageReport.getCustomerLiability().subtract(adjustedAmount);

        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        var refundResult = paymentService.refund(transactionId, refundAmount);
        
        if (refundResult.success()) {
            log.info("Refund processed successfully: damageId={}, refundAmount={}, transactionId={}", 
                    damageReport.getId(), refundAmount, refundResult.transactionId());
        } else {
            log.error("Refund failed for damage report: {}. Reason: {}", 
                    damageReport.getId(), refundResult.message());
        }
    }

    private DamageDisputeResponse buildDisputeResponse(DamageReport damageReport, BigDecimal refundAmount) {
        return new DamageDisputeResponse(
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
