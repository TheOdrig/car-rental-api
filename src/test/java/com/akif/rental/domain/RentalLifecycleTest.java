package com.akif.rental.domain;

import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.time.Duration;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.shared.exception.InvalidStatusTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Rental lifecycle")
class RentalLifecycleTest {

    private Rental rentalIn(RentalStatus status) {
        return Rental.builder().status(status).build();
    }

    @Test
    @DisplayName("confirm moves a requested rental to CONFIRMED and stores the approval notes")
    void confirmMovesRequestedRental() {
        Rental rental = rentalIn(RentalStatus.REQUESTED);

        rental.confirm("approved by desk");

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        assertThat(rental.getApprovalNotes()).isEqualTo("approved by desk");
    }

    @Test
    @DisplayName("confirm is rejected unless the rental is REQUESTED")
    void confirmRejectsOtherStatuses() {
        assertThatThrownBy(() -> rentalIn(RentalStatus.IN_USE).confirm(null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("pickUp moves a confirmed rental to IN_USE and stores the pickup notes")
    void pickUpMovesConfirmedRental() {
        Rental rental = rentalIn(RentalStatus.CONFIRMED);

        rental.pickUp("keys handed over");

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.IN_USE);
        assertThat(rental.getPickupNotes()).isEqualTo("keys handed over");
    }

    @Test
    @DisplayName("pickUp is rejected before the rental is confirmed")
    void pickUpRejectsRequestedRental() {
        assertThatThrownBy(() -> rentalIn(RentalStatus.REQUESTED).pickUp(null))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("markReturned records the return time and notes")
    void markReturnedRecordsReturn() {
        Rental rental = rentalIn(RentalStatus.IN_USE);
        Instant returnedAt = LocalDateTime.of(2026, 11, 20, 10, 30).toInstant(ZoneOffset.UTC);

        rental.markReturned(returnedAt, "no issues");

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(rental.getActualReturnTime()).isEqualTo(returnedAt);
        assertThat(rental.getReturnNotes()).isEqualTo("no issues");
    }

    @Test
    @DisplayName("cancel stores the reason and is rejected once the rental is finished")
    void cancelFollowsStatusGuard() {
        Rental rental = rentalIn(RentalStatus.CONFIRMED);

        rental.cancel("customer changed plans");

        assertThat(rental.getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(rental.getCancellationReason()).isEqualTo("customer changed plans");
        assertThatThrownBy(() -> rentalIn(RentalStatus.RETURNED).cancel("too late"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("cancel is rejected while the car is in use")
    void cancelRejectedWhileInUse() {
        assertThatThrownBy(() -> rentalIn(RentalStatus.IN_USE).cancel("keep the refund"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    @DisplayName("flagLate keeps the first detection time and updates the late hours")
    void flagLateKeepsFirstDetectionTime() {
        Rental rental = rentalIn(RentalStatus.IN_USE);
        Instant first = LocalDateTime.of(2026, 11, 20, 8, 0).toInstant(ZoneOffset.UTC);

        rental.flagLate(LateReturnStatus.LATE, first, 2);
        rental.flagLate(LateReturnStatus.SEVERELY_LATE, first.plus(Duration.ofHours(30)), 32);

        assertThat(rental.getLateReturnStatus()).isEqualTo(LateReturnStatus.SEVERELY_LATE);
        assertThat(rental.getLateDetectedAt()).isEqualTo(first);
        assertThat(rental.getLateHours()).isEqualTo(32);
    }

    @Test
    @DisplayName("registerDamageReport flags the rental and counts the reports")
    void registerDamageReportCountsReports() {
        Rental rental = rentalIn(RentalStatus.RETURNED);

        rental.registerDamageReport();
        rental.registerDamageReport();

        assertThat(rental.getHasDamageReports()).isTrue();
        assertThat(rental.getDamageReportsCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("assessPenalty and reducePenaltyTo update the penalty amount")
    void penaltyOperationsUpdateAmount() {
        Rental rental = rentalIn(RentalStatus.IN_USE);

        rental.assessPenalty(new BigDecimal("120.00"), 6);
        rental.reducePenaltyTo(new BigDecimal("40.00"));
        rental.recordPenaltyPayment(true);

        assertThat(rental.getPenaltyAmount()).isEqualByComparingTo("40.00");
        assertThat(rental.getLateHours()).isEqualTo(6);
        assertThat(rental.getPenaltyPaid()).isTrue();
    }
}
