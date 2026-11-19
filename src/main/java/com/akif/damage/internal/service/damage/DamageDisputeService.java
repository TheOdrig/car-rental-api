package com.akif.damage.internal.service.damage;

import com.akif.damage.internal.dto.damage.request.DamageDisputeRequest;
import com.akif.damage.internal.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.damage.internal.dto.damage.response.DamageDisputeResponse;
import com.akif.damage.domain.model.DamageReport;

import java.math.BigDecimal;

public interface DamageDisputeService {

    DamageDisputeResponse createDispute(Long damageId, DamageDisputeRequest request, Long userId);

    DamageDisputeResponse resolveDispute(Long damageId, DamageDisputeResolutionDto resolution, Long userId);

    void processRefundForAdjustment(DamageReport damageReport, BigDecimal adjustedAmount);
}
