package com.akif.event;

import com.akif.model.DamageReport;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DamageReportedEvent extends ApplicationEvent {

    private final DamageReport damageReport;

    public DamageReportedEvent(Object source, DamageReport damageReport) {
        super(source);
        this.damageReport = damageReport;
    }
}
