package com.akif.service.damage;

import com.akif.dto.damage.request.DamageDisputeRequestDto;
import com.akif.dto.damage.request.DamageDisputeResolutionDto;
import com.akif.dto.damage.response.DamageDisputeResponseDto;
import com.akif.model.DamageReport;

import java.math.BigDecimal;

public interface IDamageDisputeService {

    DamageDisputeResponseDto createDispute(Long damageId, DamageDisputeRequestDto request, String username);

    DamageDisputeResponseDto resolveDispute(Long damageId, DamageDisputeResolutionDto resolution, String username);

    void processRefundForAdjustment(DamageReport damageReport, BigDecimal adjustedAmount);
}
