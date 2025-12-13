# ADR-002: Cross-Module Entity Strategy

## Status
**Accepted**

## Context

We needed to decide how to manage cross-module entity relationships in the modular monolith.

**Problem:** The `Rental` entity was linked to both `User` and `Car` entities:

```java
// Old structure - Anti-pattern
@ManyToOne
private User user;  // from auth module

@ManyToOne  
private Car car;    // from car module
```

This structure violated module boundaries.

## Decision

**ID Reference + Denormalization** strategy was chosen.

## Rationale

### Alternatives Evaluated

| Strategy | Pros | Cons |
|----------|------|------|
| **@ManyToOne (Status Quo)** | Easy querying, JPA automatic | No module boundary, tight coupling |
| **ID Reference Only** | Full isolation | N+1 on every query, performance |
| **ID + Denormalization** | Isolation + Performance | Data consistency risk |
| **Shared Entity Module** | JPA relationships preserved | Shared kernel bloats |

### Why ID + Denormalization?

1. **Module Isolation:** Entities don't leak outside module
2. **Query Performance:** No JOIN needed thanks to denormalized fields
3. **Report Queries:** `rental.getCarBrand()` directly usable
4. **Referential Integrity:** Foreign keys still exist (just no @ManyToOne)

### Implementation

```java
// New structure
@Entity
public class Rental {
    @Column(name = "user_id", nullable = false)
    private Long userId;  // ID reference only
    
    @Column(name = "car_id", nullable = false)
    private Long carId;   // ID reference only
    
    // Denormalized fields (immutable at creation)
    private String carBrand;
    private String carModel;
    private String carLicensePlate;
    private String userEmail;
    private String userFullName;
}
```

### Denormalization Rules

1. **Immutable Data Only:** Only data that won't change after creation
2. **Display Only:** These fields are for display purposes only
3. **Source of Truth:** Real data still in `Car` and `User` tables
4. **No Updates:** These fields are never updated

## Consequences

### Positive
- Modules completely isolated
- Query performance high
- Report-ready data
- No entity leak in events

### Negative
- Data duplication (storage cost minimal)
- Flyway migration required
- Mappers need updating

### Migration Impact

**Note:** In this project, denormalized fields were already added during initial development. 
Below is an example of what such a migration would look like:

```sql
-- Example migration (not actually created as separate file)
ALTER TABLE rentals 
ADD COLUMN car_brand VARCHAR(100),
ADD COLUMN car_model VARCHAR(100),
ADD COLUMN car_license_plate VARCHAR(20),
ADD COLUMN user_email VARCHAR(255),
ADD COLUMN user_full_name VARCHAR(200);

-- Populate from existing data
UPDATE rentals r SET
    car_brand = (SELECT brand FROM cars WHERE id = r.car_id),
    car_model = (SELECT model FROM cars WHERE id = r.car_id),
    ...
```

## Related ADRs
- ADR-001: Spring Modulith decision
- ADR-003: Event-driven communication
