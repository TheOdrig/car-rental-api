package com.akif.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "penalty_waivers",
        indexes = {
                @Index(name = "idx_penalty_waiver_rental", columnList = "rental_id"),
                @Index(name = "idx_penalty_waiver_admin", columnList = "admin_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PenaltyWaiver extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rental_id", nullable = false, foreignKey = @ForeignKey(name = "fk_penalty_waiver_rental"))
    private Rental rental;

    @Column(name = "original_penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal originalPenalty;

    @Column(name = "waived_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal waivedAmount;

    @Column(name = "remaining_penalty", nullable = false, precision = 12, scale = 2)
    private BigDecimal remainingPenalty;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "waived_at", nullable = false)
    private LocalDateTime waivedAt;

    @Column(name = "refund_initiated")
    private Boolean refundInitiated;

    @Column(name = "refund_transaction_id")
    private String refundTransactionId;

    @Override
    public String toString() {
        return "PenaltyWaiver{" +
                "id=" + getId() +
                ", rentalId=" + (rental != null ? rental.getId() : "N/A") +
                ", originalPenalty=" + originalPenalty +
                ", waivedAmount=" + waivedAmount +
                ", remainingPenalty=" + remainingPenalty +
                ", adminId=" + adminId +
                ", waivedAt=" + waivedAt +
                ", refundInitiated=" + refundInitiated +
                '}';
    }
}
