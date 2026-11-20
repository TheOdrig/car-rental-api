# ADR-002: Cross-module entity references

**Status:** Accepted


## Context

Rentals need to identify customers and cars and display key details while the auth and car modules retain ownership of their entities.

## Decision

Store the customer and car IDs as scalar fields and enforce referential integrity with database foreign keys. Copy the car brand, model and plate and the customer's e-mail, username (`userUsername`) and name when booking. A profile event refreshes the copied customer name. [Rental](../../../src/main/java/com/akif/rental/domain/model/Rental.java) · [Booking service](../../../src/main/java/com/akif/rental/internal/service/RentalBookingService.java) · [Name refresh listener](../../../src/main/java/com/akif/rental/internal/listener/CustomerNameRefreshListener.java) · [Baseline migration](../../../src/main/resources/db/migration/V1__baseline.sql)

## Consequences

Rental responses can display copied fields without joining entities across modules. Each copied field has its own update semantics, so consumers that need current customer or car data should use the owning module's API.
