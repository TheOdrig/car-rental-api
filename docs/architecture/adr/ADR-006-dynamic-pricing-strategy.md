# ADR-006: Dynamic pricing

**Status:** Accepted


## Context

Rental quotes apply seasonal, early-booking, duration, weekend and demand adjustments to the base rate. Each adjustment has its own inputs and can be configured independently.

## Decision

Apply enabled `PricingStrategy` implementations in ascending `getOrder()`. Compose their multipliers, round amounts to the currency's decimal places, and enforce the final price floor and ceiling using the rounded average daily price. The bounds can be overridden per currency. [Pricing service](../../../src/main/java/com/akif/car/internal/service/pricing/impl/DynamicPricingServiceImpl.java)

Demand is occupied bookable car-days divided by available car-days over the inclusive quote period. Count distinct cars per day with committed `CONFIRMED` or `IN_USE` bookings. A `REQUESTED` rental blocks availability while contributing zero to demand. [Demand strategy](../../../src/main/java/com/akif/car/internal/service/pricing/strategy/DemandPricingStrategy.java) · [Booking calendar](../../../src/main/java/com/akif/rental/internal/service/RentalBookingCalendar.java)

## Consequences

Early return reprices both the full booked period and the days used, excluding the rental itself from demand. It scales the repriced used amount by the original total divided by the repriced full-period total. It falls back to a prorated share of the amount paid if the car's currency changed, whole-period repricing gives no positive total, or repricing raises a domain error. [Refund service](../../../src/main/java/com/akif/rental/internal/service/RentalRefundService.java)
