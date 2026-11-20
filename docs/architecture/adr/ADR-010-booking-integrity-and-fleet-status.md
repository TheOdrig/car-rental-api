# ADR-010: Booking integrity and fleet status

**Status:** Accepted


## Context

Concurrent requests can both observe the same available car. Fleet condition also needs to remain independent of individual rental dates.

## Decision

Use PostgreSQL's `btree_gist` extension and an exclusion constraint to prevent overlapping inclusive date ranges for the same car on active, non-deleted bookings. A booking constraint violation maps to HTTP 409. Use fleet status for vehicle condition and rental records for date availability. [Overlap migration](../../../src/main/resources/db/migration/V4__rental_overlap_exclusion.sql) · [Fleet status migration](../../../src/main/resources/db/migration/V11__car_fleet_status_only.sql) · [Booking service](../../../src/main/java/com/akif/rental/internal/service/RentalBookingService.java)

## Consequences

The migration role must be able to create `btree_gist` if the extension is absent. Pickup checks the start date, the no-show cutoff (`startDate + noShowDays - 1`), the end date and whether a different rental already has the car in use. [Rental lifecycle service](../../../src/main/java/com/akif/rental/internal/service/RentalLifecycleService.java)
