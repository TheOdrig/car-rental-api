# Git Workflow - Backend Bug Fixes

## Branch Strategy

```
main
  └── fix/backend-bug-fixes
```

## Commit Plan

### Task 1: Database Migration - Data Fix ve Index Ekleme

```
chore(db): add V15 migration for data fix and indexes

Fix "Manuel" → "Manual" inconsistency in transmission_type.
Add indexes for transmission_type, body_type, fuel_type, seats columns.

BREAKING CHANGE: Database schema updated, requires migration V15.
```

### Task 2: CarFilterRequest DTO Oluşturma

```
feat(car): add CarFilterRequest DTO

Enable filter parameter validation with size and range constraints.
Support brand, model, transmissionType, bodyType, fuelType, minSeats, minPrice, maxPrice.
```

### Task 3: CarRepository Filter Query Ekleme

```
feat(car): extend findCarsByCriteria with filter parameters

Add transmissionType, bodyType, fuelType, minSeats to existing query.
Apply case-insensitive matching with LOWER function.
```

### Task 4: CarService Bug Fixes

```
fix(car): fix NPE, cache collision, and restoreCar return type

- Use Objects.equals() for null-safe VIN comparison in updateCar
- Replace hashCode() with toString() for cache key collision prevention
- Update restoreCar to return CarResponse instead of void
```

### Task 5: CarController Authorization

```
fix(car): add admin authorization to CarController CRUD endpoints

- Add @PreAuthorize("hasRole('ADMIN')") to createCar, updateCar, deleteCar, softDeleteCar, restoreCar
- Update restoreCar to return CarResponse instead of noContent
- Add 403 Forbidden response documentation to OpenAPI specs
```

### Task 6: RentalController Authorization

```
fix(rental): add admin authorization to RentalController operations

- Add @PreAuthorize("hasRole('ADMIN')") to confirmRental, pickupRental, returnRental
- Add 403 Forbidden response documentation to OpenAPI specs
```

### Task 8: Unit Test Yazımı

```
test(car): add bug fix unit tests

- Add VIN null comparison tests (NPE prevention)
- Add restoreCar CarResponse return type tests
- Verify Objects.equals() usage for null-safe comparisons
```

### Task 9: Integration Test Yazımı

```
test(car): add CarController authorization integration tests

- Test ADMIN role access for CRUD operations
- Test USER role denial with 403 Forbidden
- Verify read operations are allowed for all users
```

```
test(rental): add RentalController authorization integration tests

- Test ADMIN role for confirmRental, pickupRental, returnRental
- Test USER role denial with 403 Forbidden
- Verify USER can still request, view, and cancel own rentals
```

## Merge & Rollback

```bash
# Merge
git checkout main
git merge fix/backend-bug-fixes

# Code Rollback (single commit)
git revert <commit-hash>

# Feature Rollback (multiple commits)
git revert --no-commit <first-commit>..<last-commit>
git commit -m "revert(car): rollback backend-bug-fixes"
```

### Migration Rollback

```sql
-- Create rollback migration: V15.1__rollback_bug_fixes.sql

-- Remove indexes
DROP INDEX IF EXISTS gallery.idx_car_transmission_type;
DROP INDEX IF EXISTS gallery.idx_car_body_type;
DROP INDEX IF EXISTS gallery.idx_car_fuel_type;
DROP INDEX IF EXISTS gallery.idx_car_seats;

-- Note: Data fix (Manuel → Manual) cannot be safely reverted
-- as original data state is unknown. Manual intervention required.
```

## Testing

```bash
# Run all car module tests
mvn test -Dtest="com.akif.car.**"

# Run all rental module tests
mvn test -Dtest="com.akif.rental.**"

# Run specific filter tests
mvn test -Dtest="*CarFilter*"

# Run authorization tests
mvn test -Dtest="*Authorization*"
```

## Key Components

| Component | Description |
|-----------|-------------|
| CarFilterRequest | DTO for filter parameter validation |
| CarRepository.findActiveCarsWithFilters | JPQL query with dynamic filtering |
| CarServiceImpl | Filter logic and cache management |
| CarController | Filter endpoints and admin authorization |
| RentalController | Admin authorization for rental operations |
| V15 Migration | Data consistency fix and index optimization |

## Checklist

- [x] Conventional Commits format used
- [x] Scopes are short (1-2 words, no hyphens)
- [x] Subject in imperative mood (max 50 chars)
- [x] Body explains WHY, not WHAT
- [x] English only in all commit messages
- [x] Breaking changes marked with `!` and footer
- [x] Each commit is atomic (single logical change)
- [x] Task-Commit alignment (subtask count = commit count per task)
- [x] Branch name follows `fix/{fix-name}` format
- [x] Merge and rollback commands included
- [x] Migration rollback documented
- [x] Test commands included
- [x] Key components table included
