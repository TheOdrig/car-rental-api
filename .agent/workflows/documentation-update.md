---
description: Documentation update checklist for new modules or features
---

# Documentation Update Workflow

Use this workflow whenever you add a **new module** or **complete a major feature**.

---

## 🔴 CRITICAL: Single Source of Truth

**README.md is the primary source.** All other docs should be consistent with it.

---

## Checklist: New Module Added

When a new module (e.g., `dashboard`) is added:

// turbo-all
1. **README.md** (PRIMARY)
   - [ ] Update Features table (module count)
   - [ ] Update Mermaid diagram (add module + dependencies)
   - [ ] Update Modules table (add row)
   - [ ] Update API Overview section (add endpoints)
   - [ ] Update Project Structure (add folder)
   - [ ] Update Technical Highlights (if applicable)

2. **docs/architecture/MIGRATION.md**
   - [ ] Update Module Inventory (add folder structure)
   - [ ] Update Module Dependencies list
   - [ ] Update Published Events table (if module publishes events)
   - [ ] Update Success Criteria table (if metrics changed)

3. **FEATURE_ROADMAP.md**
   - [ ] Mark feature as completed (✅)
   - [ ] Add implementation details
   - [ ] Update interview talking points
   - [ ] Update Faz progress section

4. **RENTAL_MVP_PLAN.md**
   - [ ] Mark feature as completed
   - [ ] Add endpoint summary
   - [ ] Add implementation notes
   - [ ] Update progress percentage table
   - [ ] Update "Sonraki Adım" section
   - [ ] Add to "Tamamlanan Spec'ler" list

5. **.kiro/specs/{feature}/tasks.md**
   - [ ] Mark all tasks as completed [x]

---

## Checklist: Feature Completed (No New Module)

When completing a feature within an existing module:

1. **FEATURE_ROADMAP.md** - Mark as completed
2. **RENTAL_MVP_PLAN.md** - Update progress
3. **.kiro/specs/{feature}/tasks.md** - Mark tasks completed

---

## Verification Commands

```bash
# Check module count consistency
# README should match MIGRATION.md

# Count modules in src
ls src/main/java/com/akif/ | wc -l

# Check README module count
grep -o "9 modules" README.md  # Update number as needed

# Check MIGRATION.md module list
grep -A 50 "Module Structure" docs/architecture/MIGRATION.md | grep "├──" | wc -l
```

---

## Common Mistakes to Avoid

1. ❌ Updating README but forgetting MIGRATION.md
2. ❌ Different module counts in different files
3. ❌ Mermaid diagram missing new modules
4. ❌ Forgetting to update year in "Son Güncelleme"
5. ❌ Leaving tasks.md tasks unchecked

---

## When to Update

| Action | Files to Update |
|--------|-----------------|
| New module | ALL (README, MIGRATION, ROADMAP, MVP_PLAN, tasks) |
| New endpoint | README (API Overview), tasks |
| Bug fix | tasks (if tracked) |
| Test added | None (unless test coverage metrics in README) |
| Refactor | tasks (if tracked) |
| Feature complete | ROADMAP, MVP_PLAN, tasks |

---

## Quick Reference: File Locations

```
README.md                           # Primary source
FEATURE_ROADMAP.md                  # Roadmap + interview points
RENTAL_MVP_PLAN.md                  # Detailed progress
docs/architecture/MIGRATION.md      # Module inventory
docs/architecture/DEVELOPER_GUIDE.md # How-to guide (rarely updated)
docs/CONFIGURATION.md               # Config options (rarely updated)
.kiro/specs/{feature}/tasks.md      # Task tracking
```
