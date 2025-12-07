package com.akif.event;

import com.akif.model.DamageReport;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

@Getter
public class DamageResolvedEvent extends ApplicationEvent {

    private final DamageReport damageReport;
    private final BigDecimal refundAmount;

    public DamageResolvedEvent(Object source, DamageReport damageReport, BigDecimal refundAmount) {
        super(source);
        this.damageReport = damageReport;
        this.refundAmount = refundAmount;
    }
}
