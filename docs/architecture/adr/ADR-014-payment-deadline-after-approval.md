# ADR-014: Payment deadline after approval

**Status:** Accepted


## Context

A rental approved close to the start date still needs a payment window after approval.

## Decision

The payment deadline is the later of midnight UTC at the start of `startDate` and `confirmedAt + rental.payment-grace-hours`. Records without a confirmation timestamp use midnight UTC at the start of `startDate`. The grace defaults to 12 hours and accepts values from 1 to 72. Invalid values fail startup. The intent endpoint rejects a new attempt at or after the deadline. [Payment window](../../../src/main/java/com/akif/rental/internal/service/PaymentWindow.java) · [Rental properties](../../../src/main/java/com/akif/rental/internal/config/RentalProperties.java)

## Consequences

The response exposes `paymentDueBy` for `CONFIRMED` rentals whose payment is neither `AUTHORIZED` nor `CAPTURED`. The expiry job checks the same deadline and synchronizes pending or failed payment attempts before deciding whether to cancel, allowing a confirmed authorization to be recognized. Payment eligibility and pickup eligibility have separate date constraints. Pickup still follows the [date bounds in ADR-010](ADR-010-booking-integrity-and-fleet-status.md). [Rental assembler](../../../src/main/java/com/akif/rental/internal/service/RentalAssembler.java) · [Expiry scheduler](../../../src/main/java/com/akif/rental/internal/scheduler/UnpaidRentalExpiryScheduler.java)
