# Git Workflow - Backend Improvements

## Branch Strategy

```
main
  └── feature/backend-improvements
```

## Commit Plan

### Task 1: Seed Data Image URLs

```
chore(db): add extended seed data with image URLs

Add V16 migration with 15 new cars and image URLs for all 20 cars.
Diverse brands (Mercedes, Tesla, Ford, etc.), varied statuses,
and placeholder images for frontend development.
```

---

### Task 2: Atomic View/Like Count Updates

```
feat(car): add atomic view/like count repository methods

Prevent race conditions with UPDATE SET count = count + 1 pattern.
Eliminates read-modify-write concurrency issues.
```

```
refactor(car): use atomic updates in CarServiceImpl

Replace read-modify-write pattern with repository atomic methods.
Thread-safe view/like count increments guaranteed.
```

---

### Task 3: Cache Eviction Optimization

```
perf(car): optimize cache eviction for updateCar

Use specific key eviction instead of allEntries=true.
Preserve unrelated cache entries on single-entity updates.
```

```
perf(car): optimize cache eviction for view/like increments

Evict only affected car's cache entries.
Reduce cache invalidation overhead.
```

---

### Task 4: Similar Cars Endpoint Fix

```
fix(api): make similar cars date parameters optional

Default to today + 30 days when dates not provided.
Enable car detail page to show similar cars without date input.
```

---

### Task 5: Featured Endpoint Path Fix

```
feat(car): add /featured endpoint to CarController

Provide direct access at /api/cars/featured.
Include currency conversion support.
```

---

### Task 6: Sorting Implementation

```
feat(car): enable sorting on getAllActiveCars endpoint

Add @PageableDefault with createTime DESC default.
Support price, brand, model, productionYear, viewCount, likeCount, rating.
```

```
docs(api): document supported sort fields in OpenAPI

Add sort parameter descriptions to Swagger documentation.
```

---

### Task 7: N+1 Query Prevention (Preparation)

```
perf(car): add @EntityGraph preparation for future relations

Add empty @EntityGraph for findByIsDeletedFalse method.
Ready for eager loading when relations are added.
```

---

### Task 9 & 10: Unit & Integration Tests (Combined)

```
test(car): add tests for backend improvements

- Unit tests: atomic update methods (view/like counts)
- Unit tests: null id validation for increment methods
- Integration tests: sorting (default, price asc, viewCount desc)
- Integration tests: /featured endpoint with pagination
- Integration tests: similar cars default date logic

Covers Tasks 9.1, 9.2, 10.1, 10.2
```

---

## Merge & Rollback

```bash
# Merge
git checkout main
git merge feature/backend-improvements

# Code Rollback
git revert <commit-hash>

# Feature Rollback (multiple commits)
git revert --no-commit <first-commit>..<last-commit>
git commit -m "revert(car): rollback backend improvements"
```

### Migration Rollback

```sql
-- Create rollback migration: V16.1__rollback_seed_images.sql
-- Only affects seed data, safe to leave or update

UPDATE gallery.car SET 
    image_url = NULL,
    thumbnail_url = NULL
WHERE license_plate IN ('34ABC123', '06DEF456', '35JKL789', '16XYZ321', '01MNO654');
```

---

## Testing

```bash
# Run all car module tests
mvn test -Dtest="com.akif.car.**"

# Run specific atomic update tests
mvn test -Dtest="*CarRepository*"

# Run integration tests
mvn test -Dtest="*CarController*Integration*"
```

---

## Key Components

| Component | Description |
|-----------|-------------|
| `V16__add_seed_images.sql` | Migration adding image URLs to seed data |
| `CarRepository` | Atomic update methods for view/like counts |
| `CarServiceImpl` | Optimized cache eviction, atomic updates |
| `CarController` | `/featured` endpoint, sorting support |
| `AvailabilitySearchController` | Optional date parameters for similar cars |

---

## Checklist

- [x] Conventional Commits format used
- [x] Scopes are short (1-2 words, no hyphens)
- [x] Subject in imperative mood (max 50 chars)
- [x] Body explains WHY, not WHAT
- [x] English only in all commit messages
- [x] Breaking changes marked (none in this spec)
- [x] Each commit is atomic
- [x] Test commits separate from implementation
- [x] No checkpoint commits (Task 8, 11 are workflow steps)
- [x] Task-Commit alignment verified
- [x] Branch name follows format
- [x] Merge and rollback commands included
- [x] Migration rollback documented
- [x] Test commands included
- [x] Key components table included
