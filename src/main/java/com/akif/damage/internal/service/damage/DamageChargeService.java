package com.akif.damage.internal.service.damage;

import com.akif.damage.domain.model.DamageReport;
import com.akif.damage.internal.dto.damage.response.DamageChargeResponse;
import com.akif.payment.api.PaymentResult;

public interface DamageChargeService {

    Object createDamageCharge(DamageReport damageReport);

    PaymentResult chargeDamage(Object damagePayment);

    PaymentResult chargeDamageById(Long damageId);

    DamageChargeResponse charge(Long damageId);

    void handleFailedDamageCharge(Object damagePayment);
}

