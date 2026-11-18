package com.akif.damage.api;

import com.akif.damage.api.DamageSeverity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public class DamageReportedEvent extends ApplicationEvent {

    private final Long damageReportId;
    private final Long rentalId;
    private final Long carId;
    private final String carLicensePlate;
    private final String customerEmail;
    private final String customerFullName;
    private final String description;
    private final DamageSeverity severity;
    private final LocalDateTime reportedAt;

    public DamageReportedEvent(Object source,
                               Long damageReportId,
                               Long rentalId,
                               Long carId,
                               String carLicensePlate,
                               String customerEmail,
                               String customerFullName,
                               String description,
                               DamageSeverity severity,
                               LocalDateTime reportedAt) {
        super(source);
        this.damageReportId = damageReportId;
        this.rentalId = rentalId;
        this.carId = carId;
        this.carLicensePlate = carLicensePlate;
        this.customerEmail = customerEmail;
        this.customerFullName = customerFullName;
        this.description = description;
        this.severity = severity;
        this.reportedAt = reportedAt;
    }
}
