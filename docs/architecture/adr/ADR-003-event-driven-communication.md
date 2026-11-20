# ADR-003: Event-driven inter-module communication

**Status:** Accepted


## Context

Rental and payment transactions produce facts that other modules can act on, such as sending a confirmation e-mail.

## Decision

Publish immutable event records from the owning module's API. For example, `RentalLifecycleService` publishes `RentalConfirmedEvent` after updating the rental within the approval transaction. The notification module handles it after commit through `@ApplicationModuleListener`. Spring Modulith persists publication state in PostgreSQL and republishes outstanding deliveries on restart. [Rental lifecycle service](../../../src/main/java/com/akif/rental/internal/service/RentalLifecycleService.java) · [E-mail listener](../../../src/main/java/com/akif/notification/internal/listener/EmailEventListener.java) · [Publication migration](../../../src/main/resources/db/migration/V6__event_publication.sql) · [Application properties](../../../src/main/resources/application.properties)

## Consequences

Listeners can complete independently of the transaction that published their event. Password-reset e-mail uses a regular event listener and stays outside the persistent publication registry so the reset link is kept out of that table. [E-mail listener](../../../src/main/java/com/akif/notification/internal/listener/EmailEventListener.java)
