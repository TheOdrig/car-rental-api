package com.akif.damage.internal.service.damage.impl;

import java.time.Clock;

import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserDto;
import com.akif.damage.api.DamageDisputedEvent;
import com.akif.damage.api.DamageResolvedEvent;
import com.akif.damage.api.DamageStatus;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DamageDisputeServiceImpl implements DamageDisputeService {

    private final Clock clock;

    private final DamageReportRepository damageReportRepository;
    private final ApplicationEventPublisher eventPublisher;

    private final UserLookup userLookup;
    private final RentalService rentalService;

    private final PaymentService paymentService;

    @Override
    @Transactional
    public DamageDisputeResponse createDispute(Long damageId, DamageDisputeRequest request, String username) {
        DamageReport damageReport = damageReportRepository.findByIdAndIsDeletedFalse(damageId)
                .orElseThrow(() -> DamageReportException.notFound(damageId));

        UserDto user = userLookup.getUserByUsername(username);

        if (!damageReport.canBeDisputed()) {
            throw DamageDisputeException.invalidStatus(damageReport.getStatus().name());
        }

        RentalSummaryDto rental = rentalService.getRentalSummaryById(damageReport.getRentalId());

        if (!rental.userId().equals(user.id())) {
            log.warn("User {} attempted to dispute damage {} but is not the rental owner (ownerId={})",
                    user.id(), damageId, rental.userId());
            throw DamageDisputeException.notOwner();
        }

        damageReport.dispute(request.reason(), request.comments(), user.id(), LocalDateTime.now(clock));
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageDisputedEvent(
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

        UserDto user = userLookup.getUserByUsername(username);

        if (damageReport.getStatus() != DamageStatus.DISPUTED) {
            throw DamageDisputeException.cannotResolve(damageReport.getStatus().name());
        }

        BigDecimal originalLiability = damageReport.getCustomerLiability();
        int places = damageReport.getCurrency() != null ? damageReport.getCurrency().getDecimalPlaces() : 2;
        BigDecimal adjustedLiability = resolution.adjustedCustomerLiability()
                .setScale(places, java.math.RoundingMode.HALF_UP);
        BigDecimal adjustedRepairCost = resolution.adjustedRepairCost() == null ? null
                : resolution.adjustedRepairCost().setScale(places, java.math.RoundingMode.HALF_UP);
        BigDecimal refundAmount = BigDecimal.ZERO;

        if (adjustedLiability.compareTo(originalLiability) < 0) {
            refundAmount = originalLiability.subtract(adjustedLiability);
            processRefundForAdjustment(damageReport, adjustedLiability);
        }

        damageReport.resolveDispute(adjustedLiability, adjustedRepairCost, resolution.resolutionNotes(),
                user.id(), LocalDateTime.now(clock));
        damageReport = damageReportRepository.save(damageReport);

        eventPublisher.publishEvent(new DamageResolvedEvent(
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

        var refundResult = paymentService.refund(transactionId, refundAmount, "damage-" + damageReport.getId() + ":dispute-refund");
        
        if (refundResult.success()) {
            log.info("Refund processed successfully: damageId={}, refundAmount={}, transactionId={}", 
                    damageReport.getId(), refundAmount, refundResult.transactionId());
        } else {
            log.error("Refund failed for damage report: {}. Reason: {}",
                    damageReport.getId(), refundResult.message());
            throw new DamageDisputeException(
                    "Dispute refund failed: " + refundResult.message(), HttpStatus.BAD_GATEWAY);
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
