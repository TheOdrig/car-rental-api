package com.akif.service.damage;

import com.akif.dto.damage.request.DamageAssessmentRequestDto;
import com.akif.dto.damage.response.DamageAssessmentResponseDto;
import com.akif.enums.DamageSeverity;

import java.math.BigDecimal;

public interface IDamageAssessmentService {

    DamageAssessmentResponseDto assessDamage(Long damageId, DamageAssessmentRequestDto request, String username);

    DamageAssessmentResponseDto updateAssessment(Long damageId, DamageAssessmentRequestDto request, String username);

    BigDecimal calculateCustomerLiability(BigDecimal repairCost, boolean hasInsurance, BigDecimal deductible);

    DamageSeverity determineSeverity(BigDecimal repairCost);
}
