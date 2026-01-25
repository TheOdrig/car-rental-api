package com.akif.damage.api;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class DamageChargeFailedEvent extends ApplicationEvent {

    private final Long damageReportId;
    private final Long rentalId;
    private final String customerEmail;
    private final BigDecimal amount;
    private final String failureReason;
    private final LocalDateTime failedAt;

    public DamageChargeFailedEvent(Object source,
            Long damageReportId,
            Long rentalId,
            String customerEmail,
            BigDecimal amount,
            String failureReason,
            LocalDateTime failedAt) {
        super(source);
        this.damageReportId = damageReportId;
        this.rentalId = rentalId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.failureReason = failureReason;
        this.failedAt = failedAt;
    }
}
