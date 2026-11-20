package com.akif.damage.internal.service.damage;

import com.akif.damage.internal.dto.damage.request.DamageAssessmentRequest;
import com.akif.damage.internal.dto.damage.response.DamageAssessmentResponse;
import com.akif.damage.api.DamageSeverity;

import com.akif.shared.enums.CurrencyType;
import java.math.BigDecimal;

public interface DamageAssessmentService {

    DamageAssessmentResponse assessDamage(Long damageId, DamageAssessmentRequest request, Long userId);

    DamageAssessmentResponse updateAssessment(Long damageId, DamageAssessmentRequest request, Long userId);

    BigDecimal calculateCustomerLiability(BigDecimal repairCost, boolean hasInsurance, BigDecimal deductible);

    DamageSeverity determineSeverity(BigDecimal repairCost, CurrencyType currency);
}
