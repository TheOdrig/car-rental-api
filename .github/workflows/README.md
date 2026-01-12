# GitHub Actions Workflows

## modulith-verify.yml

**Purpose:** Enforce Spring Modulith module boundaries and shared kernel size limits.

**Triggers:**
- Push to `main`, `develop`, or `refactor/**` branches
- Pull requests to `main` or `develop`

**Jobs:**

### 1. Verify Module Structure
Runs `ModularityTests` to ensure:
- ✅ Module boundaries are respected
- ✅ No circular dependencies
- ✅ Cross-module access only via public APIs
- ✅ Repositories are module-internal
- ✅ Service implementations are hidden

**Command:** `mvn test -Dtest=ModularityTests`

### 2. Check Shared Kernel Size
Counts classes in `com.akif.shared` package (excluding `package-info.java`).

**Thresholds:**
- ✅ **Target:** ≤ 10 classes
- ⚠️ **Warning:** > 15 classes (prints warning, continues)
- ❌ **Critical:** > 20 classes (fails build)

**Why Critical:**
Without this check, the shared kernel can grow uncontrollably, leading to a "big ball of mud" architecture.

**Command:** `find src/main/java -path "*/shared/*" -name "*.java" ! -name "package-info.java" | wc -l`

### 3. Upload Test Results
Uploads test reports as artifacts for debugging failed builds.

---

## How to Test Locally

```bash
# Run ModularityTests
mvn test -Dtest=ModularityTests

# Check shared kernel size
find src/main/java -path "*/shared/*" -name "*.java" ! -name "package-info.java" | wc -l
```

---

## What Happens on Failure?

### ModularityTests Fails
**Cause:** Module boundary violation detected.

**Examples:**
- A service in module A directly accesses a repository in module B
- Circular dependency between modules
- Internal class accessed from outside the module

**Action:** Fix the violation before merging.

### Shared Kernel Size Exceeds 20
**Cause:** Too many classes in `com.akif.shared`.

**Action:** Refactor classes to their appropriate modules.

---

## Bypassing Checks (NOT RECOMMENDED)

**DO NOT** skip these checks. They are critical for maintaining modular architecture.

If you absolutely must bypass (e.g., during emergency hotfix):
1. Create a separate branch
2. Fix the issue
3. Create a follow-up PR to fix the violation

**Remember:** Every bypassed check is technical debt.

---

**Last Updated:** 2025-12-10  
**Maintainer:** Architecture Team


---

## qodana_code_quality.yml

**Purpose:** Static code analysis for code quality, bugs, and security vulnerabilities.

**Triggers:**
- Pull requests to `main` or `develop`
- Manual workflow dispatch

**Why Only PRs?**
Qodana scans are slow (5-10 minutes). Running on every push wastes CI time.

**Configuration:**

### pr-mode: true
✅ **Enabled:** Only analyzes changed files in PRs (much faster!)

**Before:** Full project scan (5-10 minutes)  
**After:** Changed files only (1-2 minutes)

### Failure Conditions
```yaml
failureConditions:
  severityThresholds:
    critical: 0    # Fail if ANY critical issues
    high: 10       # Fail if more than 10 high severity issues
```

**Why Critical?**
Without failure conditions, Qodana never fails CI even with 100 critical bugs!

### What Qodana Checks

✅ **Code Quality:**
- Code smells
- Dead code
- Complexity issues
- Naming conventions

✅ **Bugs:**
- Null pointer exceptions
- Resource leaks
- Logic errors

✅ **Security:**
- SQL injection
- XSS vulnerabilities
- Insecure configurations

❌ **Does NOT Check:**
- Spring Modulith boundaries (use ModularityTests!)
- Shared kernel size (use modulith-verify.yml!)

---

## Workflow Execution Order

```
PR Created
    ↓
[modulith-verify.yml] (Fast - 1-2 minutes)
    ├── ModularityTests
    ├── Shared Kernel Check
    └── Unit Tests
    ↓
✅ PASS → Continue
❌ FAIL → Stop (don't run Qodana)
    ↓
[qodana_code_quality.yml] (Slow - 5-10 minutes)
    └── Qodana Scan
    ↓
✅ PASS → Merge allowed
❌ FAIL → Fix issues
```

**Why This Order?**
- Fast checks catch module violations immediately (30 seconds)
- Slow checks only run if fast checks pass (saves CI time)
- Developers get faster feedback

---

## Comparison: ModularityTests vs Qodana

| Check | ModularityTests | Qodana |
|-------|-----------------|--------|
| Module boundaries | ✅ YES | ❌ NO |
| Circular dependencies | ✅ YES | ⚠️ General only |
| Shared kernel size | ✅ YES | ❌ NO |
| Code quality | ❌ NO | ✅ YES |
| Security | ❌ NO | ✅ YES |
| Speed | 🟢 Fast (30s) | 🔴 Slow (5-10m) |

**Conclusion:** Use BOTH, but for different purposes!

---

**Last Updated:** 2025-12-10  
**Maintainer:** Architecture Team
