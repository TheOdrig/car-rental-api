package com.akif.damage.domain.model;

import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.shared.domain.BaseEntity;
import com.akif.shared.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
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
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class DamageReport extends BaseEntity {

    
    @Column(name = "rental_id", nullable = false)
    private Long rentalId;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    
    @Column(name = "car_brand", length = 50, nullable = false)
    private String carBrand;

    @Column(name = "car_model", length = 50, nullable = false)
    private String carModel;

    @Column(name = "car_license_plate", length = 11, nullable = false)
    private String carLicensePlate;

    
    @Column(name = "rental_start_date", nullable = false)
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date", nullable = false)
    private LocalDate rentalEndDate;

    @Column(name = "customer_email", length = 255, nullable = false)
    private String customerEmail;

    @Column(name = "customer_full_name", length = 255, nullable = false)
    private String customerFullName;

    @Column(name = "customer_user_id", nullable = false)
    private Long customerUserId;

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
    private Instant reportedAt;

    @Column(name = "assessed_by")
    private Long assessedBy;

    @Column(name = "assessed_at")
    private Instant assessedAt;

    @Column(name = "repair_cost_estimate", precision = 12, scale = 2)
    private BigDecimal repairCostEstimate;

    @Column(name = "customer_liability", precision = 12, scale = 2)
    private BigDecimal customerLiability;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10)
    private CurrencyType currency;

    @Column(name = "original_liability", precision = 12, scale = 2)
    private BigDecimal originalLiability;

    @Column(name = "dispute_refund_amount", precision = 12, scale = 2)
    private BigDecimal disputeRefundAmount;

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

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "dispute_reason", length = 500)
    private String disputeReason;

    @Column(name = "dispute_comments", length = 1000)
    private String disputeComments;

    @Column(name = "disputed_by")
    private Long disputedBy;

    @Column(name = "disputed_at")
    private Instant disputedAt;

    @Column(name = "resolution_notes", length = 1000)
    private String resolutionNotes;

    @Column(name = "resolved_by")
    private Long resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @OneToMany(mappedBy = "damageReport", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DamagePhoto> photos = new ArrayList<>();

    public void assess(DamageSeverity severity, DamageCategory category, BigDecimal repairCost,
                       BigDecimal liability, boolean insured, BigDecimal deductible, String notes,
                       Long assessorId, Instant assessedTime) {
        reviseAssessment(severity, category, repairCost, liability, insured, deductible, notes, assessorId,
                assessedTime);
        this.status = DamageStatus.ASSESSED;
    }

    public void reviseAssessment(DamageSeverity severity, DamageCategory category, BigDecimal repairCost,
                                 BigDecimal liability, boolean insured, BigDecimal deductible, String notes,
                                 Long assessorId, Instant assessedTime) {
        this.severity = severity;
        this.category = category;
        this.repairCostEstimate = repairCost;
        this.customerLiability = liability;
        this.insuranceCoverage = insured;
        this.insuranceDeductible = deductible;
        this.assessmentNotes = notes;
        this.assessedBy = assessorId;
        this.assessedAt = assessedTime;
    }

    public void assignCurrency(CurrencyType assigned) {
        if (assigned != null) {
            this.currency = assigned;
        }
    }

    public void dispute(String reason, String comments, Long customerId, Instant disputedTime) {
        this.disputeReason = reason;
        this.disputeComments = comments;
        this.disputedBy = customerId;
        this.disputedAt = disputedTime;
        this.status = DamageStatus.DISPUTED;
    }

    public void resolveDispute(BigDecimal adjustedLiability, BigDecimal adjustedRepairCost, BigDecimal refundAmount,
                               String notes, Long resolverId, Instant resolvedTime) {
        if (this.originalLiability == null) {
            this.originalLiability = this.customerLiability;
        }
        this.disputeRefundAmount = refundAmount;
        this.customerLiability = adjustedLiability;
        this.repairCostEstimate = adjustedRepairCost;
        this.resolutionNotes = notes;
        this.resolvedBy = resolverId;
        this.resolvedAt = resolvedTime;
        this.status = DamageStatus.RESOLVED;
    }

    public void markPaymentPending() {
        this.paymentStatus = "PENDING";
    }

    public void markCharged(String chargeTransactionId) {
        this.status = DamageStatus.CHARGED;
        this.paymentStatus = "CAPTURED";
        this.transactionId = chargeTransactionId;
    }

    public void markPaymentFailed() {
        this.paymentStatus = "FAILED";
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

    public boolean isPending() {
        return status != null && !status.isTerminal();
    }

    @Override
    public String toString() {
        return "DamageReport{" +
                "id=" + getId() +
                ", rentalId=" + rentalId +
                ", carId=" + carId +
                ", carLicensePlate='" + carLicensePlate + '\'' +
                ", customerFullName='" + customerFullName + '\'' +
                ", severity=" + severity +
                ", status=" + status +
                ", reportedAt=" + reportedAt +
                '}';
    }
}
