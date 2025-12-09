package com.akif.model;

import com.akif.shared.enums.DamageCategory;
import com.akif.shared.enums.DamageSeverity;
import com.akif.shared.enums.DamageStatus;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "damage_reports",
        indexes = {
                @Index(name = "idx_damage_reports_rental", columnList = "rental_id"),
                @Index(name = "idx_damage_reports_car", columnList = "car_id"),
                @Index(name = "idx_damage_reports_status", columnList = "status"),
                @Index(name = "idx_damage_reports_reported_at", columnList = "reported_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DamageReport extends BaseEntity {
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, foreignKey = @ForeignKey(name = "fk_damage_report_rental"))
    private Rental rental;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false, foreignKey = @ForeignKey(name = "fk_damage_report_car"))
    private Car car;
    
    @Column(name = "description", nullable = false, length = 1000)
    private String description;
    
    @Column(name = "damage_location", length = 200)
    private String damageLocation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", length = 20)
    private DamageSeverity severity;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private DamageCategory category;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DamageStatus status = DamageStatus.REPORTED;
    
    @Column(name = "reported_by", nullable = false)
    private Long reportedBy;
    
    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;
    
    @Column(name = "assessed_by")
    private Long assessedBy;
    
    @Column(name = "assessed_at")
    private LocalDateTime assessedAt;
    
    @Column(name = "repair_cost_estimate", precision = 12, scale = 2)
    private BigDecimal repairCostEstimate;
    
    @Column(name = "customer_liability", precision = 12, scale = 2)
    private BigDecimal customerLiability;
    
    @Column(name = "insurance_coverage")
    private Boolean insuranceCoverage;
    
    @Column(name = "insurance_deductible", precision = 12, scale = 2)
    private BigDecimal insuranceDeductible;

    @Column(name = "assessment_notes", length = 1000)
    private String assessmentNotes;
    
    @Column(name = "payment_id")
    private Long paymentId;
    
    @Column(name = "payment_status", length = 20)
    private String paymentStatus;
    
    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;
    
    @Column(name = "dispute_comments", length = 1000)
    private String disputeComments;

    @Column(name = "disputed_by")
    private Long disputedBy;

    @Column(name = "disputed_at")
    private LocalDateTime disputedAt;
    
    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;
    
    @Column(name = "resolved_by")
    private Long resolvedBy;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @OneToMany(mappedBy = "damageReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DamagePhoto> photos = new ArrayList<>();


    public void updateStatus(DamageStatus newStatus) {
        this.status = newStatus;
    }

    public boolean canBeAssessed() {
        return status != null && status.canBeAssessed();
    }

    public boolean canBeCharged() {
        return status != null && status.canBeCharged();
    }

    public boolean canBeDisputed() {
        return status != null && status.canBeDisputed();
    }
}
