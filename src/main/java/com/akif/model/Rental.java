package com.akif.model;

import com.akif.enums.CurrencyType;
import com.akif.enums.RentalStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "rentals",
        indexes = {
                @Index(name = "idx_rentals_car", columnList = "car_id"),
                @Index(name = "idx_rentals_user", columnList = "user_id"),
                @Index(name = "idx_rentals_status", columnList = "status"),
                @Index(name = "idx_rentals_dates", columnList = "start_date, end_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Rental extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rental_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rental_car"))
    private Car car;

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

    @Column(name = "pickup_reminder_sent")
    @Builder.Default
    private boolean pickupReminderSent = false;

    @Column(name = "return_reminder_sent")
    @Builder.Default
    private boolean returnReminderSent = false;


    public void updateStatus(RentalStatus newStatus) {
        this.status = newStatus;
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
                ", carId=" + (car != null ? car.getId() : "N/A") +
                ", userId=" + (user != null ? user.getId() : "N/A") +
                '}';
    }
}
