# ADR-007: Late-return penalties

**Status:** Accepted


## Context

A late return needs a consistent due time, grace period and penalty across supported currencies.

## Decision

Measure from 23:59:59 UTC on the scheduled end date. Truncate elapsed time to whole minutes, subtract the configured grace and round positive remaining minutes up to whole hours. Apply the hourly tier through its configured maximum, the single-day rate through its maximum, and then the daily rate for late days rounded up. The default boundaries are six and 24 billable hours. Severe status starts at 24 billable hours by default, independently of the rate tier. [LateHours](../../../src/main/java/com/akif/shared/time/LateHours.java) · [Penalty calculation](../../../src/main/java/com/akif/rental/internal/service/penalty/impl/PenaltyCalculationServiceImpl.java)

## Consequences

The penalty is capped at a multiple of the rental's daily rate and rounded to the rental currency. Invalid settings fail startup. A waiver closes outstanding penalty payment attempts before recording the waiver. It can trigger a refund for a collected penalty or collection of the remaining unpaid balance. Unpaid rental expiry follows the separate [payment deadline rule](ADR-014-payment-deadline-after-approval.md). [Penalty configuration](../../CONFIGURATION.md#late-return-penalties) · [Waiver service](../../../src/main/java/com/akif/rental/internal/service/penalty/impl/PenaltyWaiverServiceImpl.java)
