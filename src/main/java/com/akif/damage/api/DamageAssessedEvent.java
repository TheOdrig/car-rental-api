package com.akif.damage.api;

import com.akif.damage.api.DamageSeverity;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class DamageAssessedEvent extends ApplicationEvent {

    private final Long damageReportId;
    private final Long rentalId;
    private final Long carId;
    private final String carLicensePlate;
    private final String customerEmail;
    private final DamageSeverity severity;
    private final BigDecimal repairCostEstimate;
    private final BigDecimal customerLiability;
    private final LocalDateTime assessedAt;

    public DamageAssessedEvent(Object source,
                               Long damageReportId,
                               Long rentalId,
                               Long carId,
                               String carLicensePlate,
                               String customerEmail,
                               DamageSeverity severity,
                               BigDecimal repairCostEstimate,
                               BigDecimal customerLiability,
                               LocalDateTime assessedAt) {
        super(source);
        this.damageReportId = damageReportId;
        this.rentalId = rentalId;
        this.carId = carId;
        this.carLicensePlate = carLicensePlate;
        this.customerEmail = customerEmail;
        this.severity = severity;
        this.repairCostEstimate = repairCostEstimate;
        this.customerLiability = customerLiability;
        this.assessedAt = assessedAt;
    }
}
