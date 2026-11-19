package com.akif.rental.domain.model;

import com.akif.shared.enums.CurrencyType;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.api.RentalStatus;
import com.akif.shared.exception.InvalidStatusTransitionException;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "rentals", indexes = {
        @Index(name = "idx_rentals_car", columnList = "car_id"),
        @Index(name = "idx_rentals_user", columnList = "user_id"),
        @Index(name = "idx_rentals_status", columnList = "status"),
        @Index(name = "idx_rentals_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_rentals_car_license_plate", columnList = "car_license_plate"),
        @Index(name = "idx_rentals_user_email", columnList = "user_email")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Rental extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "car_id", nullable = false)
    private Long carId;

    @Column(name = "car_brand", length = 50, nullable = false)
    private String carBrand;

    @Column(name = "car_model", length = 50, nullable = false)
    private String carModel;

    @Column(name = "car_license_plate", length = 11, nullable = false)
    private String carLicensePlate;

    @Column(name = "car_thumbnail_url", length = 500)
    private String carThumbnailUrl;

    @Column(name = "user_email", length = 255, nullable = false)
    private String userEmail;

    @Column(name = "user_full_name", length = 255, nullable = false)
    private String userFullName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "days", nullable = false)
    private Integer days;

    @Enumerated(EnumType.STRING)
    @Column(name = "currency", length = 10, nullable = false)
    private CurrencyType currency;

    @Column(name = "daily_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyPrice;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private RentalStatus status = RentalStatus.REQUESTED;

    @Column(name = "pickup_notes", columnDefinition = "TEXT")
    private String pickupNotes;

    @Column(name = "return_notes", columnDefinition = "TEXT")
    private String returnNotes;

    @Column(name = "approval_notes", columnDefinition = "TEXT")
    private String approvalNotes;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "pickup_reminder_sent")
    @Builder.Default
    private boolean pickupReminderSent = false;

    @Column(name = "return_reminder_sent")
    @Builder.Default
    private boolean returnReminderSent = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "late_return_status", length = 20)
    private LateReturnStatus lateReturnStatus;

    @Column(name = "late_detected_at")
    private Instant lateDetectedAt;

    @Column(name = "actual_return_time")
    private Instant actualReturnTime;

    @Column(name = "late_hours")
    private Integer lateHours;

    @Column(name = "penalty_amount", precision = 12, scale = 2)
    private BigDecimal penaltyAmount;

    @Column(name = "penalty_paid")
    @Builder.Default
    private Boolean penaltyPaid = false;

    @Column(name = "has_damage_reports")
    @Builder.Default
    private Boolean hasDamageReports = false;

    @Column(name = "damage_reports_count")
    @Builder.Default
    private Integer damageReportsCount = 0;

    public void confirm(String notes) {
        requireTransition(status.canConfirm(), RentalStatus.CONFIRMED);
        this.status = RentalStatus.CONFIRMED;
        this.approvalNotes = notes;
    }

    public void pickUp(String notes) {
        requireTransition(status.canPickup(), RentalStatus.IN_USE);
        this.status = RentalStatus.IN_USE;
        this.pickupNotes = notes;
    }

    public void markReturned(Instant returnedAt, String notes) {
        requireTransition(status.canReturn(), RentalStatus.RETURNED);
        this.status = RentalStatus.RETURNED;
        this.actualReturnTime = returnedAt;
        this.returnNotes = notes;
    }

    public void cancel(String reason) {
        requireTransition(status.canCancel(), RentalStatus.CANCELLED);
        this.status = RentalStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public void markPickupReminderSent() {
        this.pickupReminderSent = true;
    }

    public void markReturnReminderSent() {
        this.returnReminderSent = true;
    }

    public void flagLate(LateReturnStatus newStatus, Instant detectedAt, int hoursLate) {
        this.lateReturnStatus = newStatus;
        if (this.lateDetectedAt == null && newStatus != LateReturnStatus.ON_TIME) {
            this.lateDetectedAt = detectedAt;
        }
        this.lateHours = hoursLate;
    }

    public void assessPenalty(BigDecimal amount, int hoursLate) {
        this.penaltyAmount = amount;
        this.lateHours = hoursLate;
    }

    public void recordPenaltyPayment(boolean paid) {
        this.penaltyPaid = paid;
    }

    public void reducePenaltyTo(BigDecimal remainingPenalty) {
        this.penaltyAmount = remainingPenalty;
    }

    public void registerDamageReport() {
        this.hasDamageReports = true;
        this.damageReportsCount = (damageReportsCount != null ? damageReportsCount : 0) + 1;
    }

    private void requireTransition(boolean allowed, RentalStatus target) {
        if (!allowed) {
            throw new InvalidStatusTransitionException(
                    "Cannot move rental from " + status.name() + " to " + target.name());
        }
    }

    public boolean isInProgress() {
        return status == RentalStatus.REQUESTED ||
                status == RentalStatus.CONFIRMED ||
                status == RentalStatus.IN_USE;
    }

    public boolean isCompleted() {
        return status == RentalStatus.RETURNED || status == RentalStatus.CANCELLED;
    }

    @Override
    public String toString() {
        return "Rental{" +
                "id=" + getId() +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", days=" + days +
                ", dailyPrice=" + dailyPrice +
                ", totalPrice=" + totalPrice +
                ", currency=" + currency +
                ", status=" + status +
                ", carId=" + carId +
                ", carBrand='" + carBrand + '\'' +
                ", carModel='" + carModel + '\'' +
                ", carLicensePlate='" + carLicensePlate + '\'' +
                ", userId=" + userId +
                ", userEmail='" + userEmail + '\'' +
                ", userFullName='" + userFullName + '\'' +
                '}';
    }
}
