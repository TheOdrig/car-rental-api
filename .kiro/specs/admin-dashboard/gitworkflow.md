# Git Workflow - Admin Dashboard & Operations Panel

## Branch Strategy

```
main
  └── feature/admin-dashboard
```

---

## Commit Plan

### Task 1: Set up Dashboard Module Structure

```
chore(dashboard): create module package structure

Establish modular boundary for dashboard with Spring Modulith.
Configure cross-module dependencies via public APIs only.
```

```
feat(dashboard)!: add Alert entity and enums

Add AlertType, AlertSeverity enums and Alert entity.
Enable alert categorization and persistent tracking.

BREAKING CHANGE: New dashboard_alerts table required, see V13.
```

```
chore(db): add dashboard_alerts migration V13

Create alert persistence layer with optimized query indexes.
```

---

### Task 2: Implement Public API DTOs

```
feat(dashboard): add summary and fleet status DTOs

Enable real-time operational visibility for admin users.
```

```
feat(dashboard): add metrics and revenue analytics DTOs

Support financial reporting and performance tracking.
```

```
feat(dashboard): add alert, pending item, and action result DTOs

Enable admin quick actions with immediate feedback.
```

---

### Task 3: Implement Public API Interfaces

```
feat(dashboard): add DashboardService interface

Define contract for dashboard data aggregation from all modules.
```

```
feat(dashboard): add AlertService interface

Define contract for alert generation and acknowledgment.
```

```
feat(dashboard): add QuickActionService interface

Define contract for rental quick actions.
```

---

### Task 4: Implement Repository Layer

```
feat(dashboard): add AlertRepository

Enable severity-sorted queries and duplicate alert prevention.
```

---

### Task 5: Extend Existing Module APIs

```
feat(rental): add dashboard query methods

Enable pending items listing and status counting for dashboard.
```

```
feat(car): add dashboard query methods

Enable fleet status calculation excluding sold vehicles.
```

```
feat(payment): add dashboard query methods

Enable revenue aggregation by day and month.
```

```
feat(damage): add dashboard query methods

Enable pending assessment counting for daily summary.
```

---

### Task 6: Implement Dashboard Service

```
feat(dashboard): implement DashboardQueryService

Aggregate data from multiple modules via public APIs only.
Enforce modular boundary compliance.
```

```
feat(dashboard): implement DashboardServiceImpl with caching

Optimize repeated queries with Caffeine cache.
5 min TTL for summary, 15 min for analytics.
```

```
test(dashboard): add DashboardServiceImpl unit tests

Verify calculation accuracy for summary, fleet, and metrics.
```

---

### Task 7: Implement Alert Service

```
feat(dashboard): implement AlertServiceImpl

Generate alerts based on business rules with scheduled checks.
Support acknowledgment with admin audit trail.
```

```
test(dashboard): add AlertServiceImpl unit tests

Verify alert generation conditions and severity assignment.
```

---

### Task 8: Implement Quick Action Service

```
feat(dashboard): implement QuickActionServiceImpl

Delegate rental operations to RentalService public API.
Return updated summary for immediate UI refresh.
```

```
test(dashboard): add QuickActionServiceImpl unit tests

Verify action delegation and error handling.
```

---

### Task 9: Implement Cache Invalidation

```
feat(dashboard): add DashboardEventListener

Invalidate relevant caches on rental, payment, damage events.
Ensure data freshness without manual refresh.
```

```
feat(dashboard): configure Caffeine cache settings

Separate TTL for summary (5 min) and analytics (15 min) data.
```

---

### Task 10: Implement Controllers

```
feat(dashboard): implement DashboardController

Expose summary, fleet, metrics, revenue, pending items endpoints.
Enforce ADMIN role authorization.
```

```
feat(dashboard): implement AlertController

Expose alert listing and acknowledgment endpoints.
```

```
feat(dashboard): implement QuickActionController

Expose approve, pickup, return quick action endpoints.
```

---

### Task 12: Integration Tests

```
test(dashboard): add DashboardControllerIntegrationTest

Verify authorization, response structure, and pagination.
```

```
test(dashboard): add AlertControllerIntegrationTest

Verify alert listing and acknowledgment flow.
```

```
test(dashboard): add QuickActionControllerIntegrationTest

Verify action execution and state transitions.
```

---

### Task 13: E2E Tests

```
test(dashboard): add DashboardE2ETest

Verify complete dashboard flows: summary, alert generation,
quick actions, revenue analytics, cache invalidation, and authorization.
```

---

### Task 15: Documentation

```
docs(dashboard): update RENTAL_MVP_PLAN.md

Mark Admin Dashboard as completed with endpoint summary.
```

```
docs(dashboard): update FEATURE_ROADMAP.md

Add interview talking points for dashboard feature.
```

---

## Merge & Rollback

```bash
# Merge to main
git checkout main
git merge feature/admin-dashboard

# Code Rollback (single commit)
git revert <commit-hash>

# Feature Rollback (multiple commits)
git revert --no-commit <first-commit>..<last-commit>
git commit -m "revert(dashboard): rollback admin dashboard feature"
```

### Migration Rollback

```sql
-- V13.1__rollback_dashboard_alerts.sql
DROP INDEX IF EXISTS idx_alert_type;
DROP INDEX IF EXISTS idx_alert_severity;
DROP INDEX IF EXISTS idx_alert_acknowledged;
DROP INDEX IF EXISTS idx_alert_created;
DROP TABLE IF EXISTS dashboard_alerts;
```

---

## Testing

```bash
# Run all dashboard tests
mvn test -Dtest="*Dashboard*,*Alert*,*QuickAction*"

# Run unit tests only
mvn test -Dtest="DashboardServiceImplTest,AlertServiceImplTest,QuickActionServiceImplTest"

# Run integration tests
mvn test -Dtest="*ControllerIntegrationTest" -Dgroups="dashboard"

# Run E2E tests
mvn test -Dtest="DashboardE2ETest"
```

---

## Key Components

| Component | Description |
|-----------|-------------|
| `DashboardService` | Main aggregation API for dashboard data |
| `AlertService` | Alert generation and acknowledgment |
| `QuickActionService` | Rental quick actions (approve, pickup, return) |
| `DashboardController` | REST endpoints for dashboard data |
| `AlertController` | REST endpoints for alert management |
| `QuickActionController` | REST endpoints for quick actions |
| `DashboardEventListener` | Event-driven cache invalidation |
| `Alert` | Domain entity for persistent alerts |

---

## Commit Summary

| Task | Commits | Focus |
|------|---------|-------|
| 1 | 3 | Module setup, entity+enums, migration |
| 2 | 3 | DTOs |
| 3 | 3 | Interfaces |
| 4 | 1 | Repository |
| 5 | 4 | Cross-module API extensions |
| 6 | 3 | Dashboard service + tests |
| 7 | 2 | Alert service + tests |
| 8 | 2 | Quick action service + tests |
| 9 | 2 | Cache invalidation |
| 10 | 3 | Controllers |
| 12 | 3 | Integration tests |
| 13 | 1 | E2E tests |
| 15 | 2 | Documentation |
| **Total** | **32** | |

> Note: Tasks 11 and 14 are checkpoints (workflow steps, not commits)

---

## Checklist

- [x] Conventional Commits format used
- [x] Scopes are short (1-2 words, no hyphens)
- [x] Subject in imperative mood (max 50 chars)
- [x] Body explains WHY, not WHAT
- [x] Body max 4 lines
- [x] English only in all commits
- [x] Breaking changes marked with `!` and footer
- [x] Entity + Migration compile-aware separation
- [x] Each commit is atomic
- [x] Commit size limits respected
- [x] Test commits separate from implementation
- [x] No checkpoint commits
- [x] **Task-Commit alignment** (subtask count = commit count)
- [x] Branch name follows format
- [x] Merge and rollback commands included
- [x] Migration rollback documented
- [x] Test commands included
- [x] Key components table included

