# ADR-001: Spring Modulith over Maven Multi-Module

## Status
**Accepted**

## Context

The Car Rental API started as a monolithic Spring Boot application. As the codebase grew (8 main domains), the need for modular structure emerged.

Two options were evaluated:

1. **Maven Multi-Module Project**
   - Each module as separate Maven artifact
   - Compile-time isolation
   - Separate deployment capability
   
2. **Spring Modulith (Single Module)**
   - Package-based modules
   - Runtime verification
   - Single artifact, easy deployment

## Decision

**Spring Modulith** was chosen.

## Rationale

### Disadvantages of Maven Multi-Module

1. **Operational Complexity:** Separate `pom.xml` for each module, version management complexity
2. **Build Time:** Full rebuild must compile every module
3. **Premature Optimization:** No current plan for microservice transition
4. **Refactoring Friction:** Moving code between modules is very difficult
5. **Local Development:** IDE setup is more complex

### Advantages of Spring Modulith

1. **Zero Build Overhead:** Single Maven module, fast build
2. **Gradual Migration:** Existing code could be reorganized incrementally
3. **Compile + Runtime Checks:** `ApplicationModules.verify()` provides both compile and runtime verification
4. **Documentation Generation:** PlantUML diagrams are automatic
5. **Event Abstraction:** Loosely coupled communication with `ApplicationEventPublisher`
6. **Future-Proof:** Easy transition to microservices later (clear module boundaries)

### Risk Assessment

| Risk | Mitigation |
|------|------------|
| Discipline required | CI pipeline with `ModularityTests` |
| No compile-time package isolation | Runtime verification + code review |
| Single deployment | Acceptable for current scale |

## Consequences

### Positive
- Fast migration
- Existing tests preserved
- Single artifact deployment
- Easy refactoring in IDE

### Negative
- Wrong imports compile (but caught at runtime)
- Team discipline required
- CI pipeline mandatory

## Related ADRs
- ADR-002: Cross-module entity strategy
- ADR-003: Event-driven inter-module communication
