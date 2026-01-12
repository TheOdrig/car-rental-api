# Git Workflow Generator - AI Agent Instructions

Instructions for generating professional git-workflow.md files from spec documents.

## Usage

Read `requirements.md`, `design.md`, `tasks.md` from the spec folder, then apply these rules.

---

## Commit Message Format (Conventional Commits)

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type (Required)
| Type | Usage |
|------|-------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Code refactoring (no behavior change) |
| `test` | Adding/fixing tests |
| `docs` | Documentation |
| `chore` | Build, config, migration |
| `perf` | Performance improvement |
| `style` | Formatting (no behavior change) |

### Scope (Required)
- **1-2 words**, keep it short
- No hyphens: ❌ `late-return` → ✅ `penalty`
- Domain-based: `rental`, `payment`, `penalty`, `email`, `api`, `db`, `auth`

### Subject (Required)
- **Imperative mood**: "add", "implement", "update", "fix"
- ❌ "added", "adds", "adding"
- Lowercase, no period
- Max 50 characters

### Body (Optional)
- Explain **WHY**, not **WHAT** (code shows what, commit explains why)
- ❌ "Add rentalId, customerId, status fields" (listing fields)
- ✅ "Enable rental tracking with customer association"
- 72 character wrap
- Max 3-4 lines, be concise

### Language Rule
- **English only** in commit messages and body
- Git history must be internationally readable


### Footer (When needed)
- `BREAKING CHANGE:` - API/schema change
- `Refs: #123` - Issue reference
- `Co-authored-by:` - Pair programming

---

## Breaking Change Rules

**When is it a breaking change?**
- Adding new fields to Entity (requires migration)
- API endpoint changes
- DTO field additions/removals
- Enum value additions/removals

**Format:**
```
feat(rental)!: extend Rental entity with late return fields

Add lateReturnStatus, lateDetectedAt, penaltyAmount fields.

BREAKING CHANGE: Rental entity schema updated, requires migration V8.
```

---

## Task-Commit Alignment Rule

> ⚠️ **KRİTİK: Her tasks.md subtask'ı = 1 commit**

### Golden Rule
```
tasks.md subtask count = gitworkflow.md commit count (per task)
```

### Neden?
- Progress tracking kolaylaşır
- "Task X.Y bitti" = "Commit Z yapıldı" net eşleşme
- Documentation tutarlılığı

### Doğru Örnek
```markdown
# tasks.md
- [ ] 1.1 Create enums
- [ ] 1.2 Create entity
- [ ] 1.3 Create migration

# gitworkflow.md (3 commit)
feat(x): add enums
feat(x): add entity
chore(db): add migration
```

### Yanlış Örnek
```markdown
# tasks.md
- [ ] 1.2 Create entity and enums  ← 1 subtask

# gitworkflow.md (2 commit)  ← UYUMSUZ!
feat(x): add enums
feat(x): add entity
```

### Çözüm
Eğer commit'i split etmek istiyorsan → **önce tasks.md'yi güncelle**.

---

## Atomic Commit Rules

### Commit Together
- Entity + Migration (same commit) - **Exception**: See Compile-Aware rule below
- Interface + Implementation (separate OK, but together also fine)
- Controller + OpenAPI docs (same commit)

### Commit Separately
- Different domains (penalty vs email)
- Different layers (service vs controller)
- Tests (separate from implementation)

### Commit Size Limits
| Item | Max per Commit |
|------|----------------|
| Endpoints | 4 (group by logical function) |
| Test scenarios | 3-4 (group by feature area) |
| DTOs | 3-4 (group by related functionality) |
| Event listeners | 2-3 (same event type) |

> **Rule of thumb**: If commit body needs more than 4 lines to explain, split it.

### What is NOT a Commit
These are **workflow steps**, not commits:
- ❌ "Verify compilation" → This is CI's job
- ❌ "Checkpoint - ensure tests pass" → Workflow step, not code change
- ❌ "Review and validate" → Process step

> Only commit when there's actual code change.

### Compile-Aware Exception

> ⚠️ **Her task sonunda proje MUTLAKA compile olmalı**

When task dependencies require it, Entity and Migration may be in **separate commits**
if they are in different "compile units". This happens when:
- Interfaces reference DTOs/Entities that don't exist yet
- Entities are needed for interface compilation, but migration is a separate concern

**Example:**
```
Task 1: Entities + DTOs + Interfaces (compile unit - all types together)
Task 2: Migration + Config (infrastructure - can run independently)
```

**When using this exception, add a note in the commit:**
```
Note: Entity created in Task 1 for interface compilation.
Migration added separately for infrastructure separation.
```

### Decision Matrix
| Change | Commit Strategy |
|--------|-----------------|
| Enum addition | Single commit |
| Config class | Single commit |
| Entity + Migration | Together (or separate if compile-aware) |
| Service interface + impl | Together or separate |
| Multiple events | Together (same domain) |
| Email templates | Together |
| Controller + docs | Together |
| Unit tests | Group by domain |
| Integration tests | Group by controller |


---

## Git Workflow Template

```markdown
# Git Workflow - {Feature Name}

## Branch Strategy

\`\`\`
main
  └── feature/{feature-name}
\`\`\`

## Commit Plan

### Task {N}: {Task Name}
\`\`\`
{type}({scope}): {subject}

{body - explain WHY, not WHAT}
\`\`\`

## Merge & Rollback

\`\`\`bash
# Merge
git checkout main
git merge feature/{feature-name}

# Code Rollback
git revert <commit-hash>

# Feature Rollback (multiple commits)
git revert --no-commit <first-commit>..<last-commit>
git commit -m "revert({scope}): rollback {feature-name}"
\`\`\`

### Migration Rollback (if applicable)
\`\`\`sql
-- Create rollback migration: V{N}.1__rollback_{feature}.sql
-- Include DROP TABLE, DROP INDEX statements
-- Document in rollback section which migration to target
\`\`\`

## Testing

\`\`\`bash
# Run feature tests
mvn test -Dtest="*{FeatureName}*"
\`\`\`

## Key Components

| Component | Description |
|-----------|-------------|
| {Component1} | {Description} |
```

---

## Example Transformation

### Input (from tasks.md):
```
- [ ] 1. Set up core infrastructure
  - [ ] 1.1 Create LateReturnStatus enum
  - [ ] 1.2 Create PenaltyConfig configuration class
  - [ ] 1.3 Extend Rental entity with late return fields
  - [ ] 1.4 Create Flyway migration
  - [ ] 1.5 Create PenaltyWaiver entity
  - [ ] 1.6 Create Flyway migration for PenaltyWaiver
  - [ ] 1.7 Create PenaltyWaiverRepository
```

### Output (git-workflow.md):
```markdown
### Task 1: Core Infrastructure

\`\`\`
feat(penalty): add LateReturnStatus enum

Add enum with ON_TIME, GRACE_PERIOD, LATE, SEVERELY_LATE values.
\`\`\`

\`\`\`
feat(penalty): add PenaltyConfig configuration

Add @ConfigurationProperties for grace period and penalty rates.
\`\`\`

\`\`\`
feat(rental)!: extend Rental entity with late return fields

Add lateReturnStatus, lateDetectedAt, penaltyAmount fields.

BREAKING CHANGE: Rental entity schema updated, requires migration.
\`\`\`

\`\`\`
feat(penalty): add PenaltyWaiver entity and repository

Add entity for tracking penalty waivers with audit trail.
Include Flyway migration V9.
\`\`\`
```

---

## Scope Mapping (Project-Specific)

| Domain | Scope |
|--------|-------|
| Rental | `rental` |
| Payment | `payment` |
| Penalty/Late return | `penalty` |
| Damage Management | `damage` |
| File Storage | `storage` |
| Email | `email` |
| REST API | `api` |
| Database | `db` |
| Authentication | `auth` |
| Car | `car` |
| User | `user` |
| Pricing | `pricing` |
| Currency | `currency` |

---

## Checklist (For every git-workflow.md)

- [ ] Conventional Commits format used
- [ ] Scopes are short (1-2 words, no hyphens)
- [ ] Subject in imperative mood (max 50 chars)
- [ ] Body explains WHY, not WHAT (no field listings)
- [ ] Body wrapped at 72 characters, max 4 lines
- [ ] **English only** in all commit messages
- [ ] Breaking changes marked with `!` and footer
- [ ] Entity + Migration in same commit (or with compile-aware note)
- [ ] Each commit is atomic (single logical change)
- [ ] Commit size limits respected (max 4 endpoints, 3-4 tests per commit)
- [ ] Test commits separate from implementation
- [ ] **No checkpoint commits** (verification is workflow, not code)
- [ ] **Task-Commit alignment** (subtask count = commit count per task)
- [ ] Branch name follows `feature/{feature-name}` format
- [ ] Merge and rollback commands included
- [ ] **Migration rollback documented** (if DB changes exist)
- [ ] Test commands included
- [ ] Key components table included

---

## Anti-Patterns (Avoid These)

### ❌ Field Listing in Body
```
feat(rental): add Rental entity

Add rentalId, customerId, carId, startDate, endDate, status, 
totalAmount, createdAt, updatedAt fields.
```
**Fix**: Remove field list, explain purpose instead.

### ❌ Checkpoint Commits
```
chore(dashboard): verify all tests passing
```
**Fix**: Delete. This is CI's job, not a commit.

### ❌ Mixed Language
```
feat(dashboard): implement AlertController

Alert yönetimi için endpoints eklendi.
```
**Fix**: English only in git history.

### ❌ Kitchen Sink Commit
```
feat(dashboard): implement all controllers and services

Add DashboardController, AlertController, QuickActionController,
DashboardService, AlertService, QuickActionService...
```
**Fix**: Split by component type or domain area.

### ❌ Missing Migration Rollback
```
## Rollback
git revert <hash>
```
**Fix**: Add `V{N}.1__rollback_{feature}.sql` path and strategy.

### ❌ Task-Commit Mismatch
```
# tasks.md: 1.2 Create entity and enums (1 subtask)
# gitworkflow.md: 2 commits for same task
```
**Fix**: Either combine commits or split the task in tasks.md first.

