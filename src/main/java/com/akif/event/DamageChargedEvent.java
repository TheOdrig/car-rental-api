package com.akif.event;

import com.akif.model.DamageReport;
import com.akif.model.Payment;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DamageChargedEvent extends ApplicationEvent {

    private final DamageReport damageReport;
    private final Payment payment;

    public DamageChargedEvent(Object source, DamageReport damageReport, Payment payment) {
        super(source);
        this.damageReport = damageReport;
        this.payment = payment;
    }
}
