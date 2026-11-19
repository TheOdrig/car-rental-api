package com.akif.payment.internal.listener;

import com.akif.payment.internal.service.CapturedPaymentRefunder;
import com.akif.payment.internal.service.RefundRequiredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRequiredListener {

    private final CapturedPaymentRefunder refunder;

    @ApplicationModuleListener
    public void onRefundRequired(RefundRequiredEvent event) {
        refunder.refund(event.paymentId(), event.cause());
    }
}
