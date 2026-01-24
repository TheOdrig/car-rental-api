# Plan: Update Project Documentation

## Goal
Update core backend documentation in `docs/` to match recent code changes (Migrations, Email Styling, User Settings), ensuring consistency and accuracy. Frontend documentation is excluded.

## Tasks
- [x] **Update Deployment Guide** (`docs/operations/DEPLOYMENT.md`)
    - [x] Update migration file list to reflect the consolidation (20 -> 7 files).
    - [x] Verify deployment steps are still accurate with the new migration structure.
- [x] **Update Email Guide** (`docs/EMAIL_GUIDE.md`)
    - [x] Update "Brand Identity" section with new color palette.
    - [x] Update "Dark Mode Support" section with new CSS strategy (e.g., usage of `text-muted` vs `.text-label`).
- [x] **Update Auth & Testing Docs** (`docs/security/AUTHENTICATION.md`, `docs/testing/TEST_STRATEGY.md`)
    - [x] Add "User Account Settings" (Avatar, Profile Update, Password Reset) to Authentication scope if missing.
    - [x] Update Test Strategy to include new E2E tests (Currency, Damage, Rental Lifecycle) and property-based testing requirements.
    - [x] Integrate latest JaCoCo coverage metrics (Jan 2026 baseline).

- [x] **Update Architecture & API Docs** (`docs/architecture/`, `docs/api/`)
    - [x] Check if "Similar Cars" and "User Settings" API endpoints are documented.
    - [x] Add missing ADRs or API definitions if critical backend concepts are undocumented.
    - [x] Update `ERROR_CODES.md` with new Avatar and Profile error codes.
    - [x] Update `CONFIGURATION.md` with File Storage (R2) settings.

## Done When
- [x] `DEPLOYMENT.md` accurately lists the 7 consolidated migration files.
- [x] `EMAIL_GUIDE.md` reflects the current CSS/Color logic used in templates.
- [x] Security and Testing docs mention the new User/Avatar features and E2E coverage.
- [x] `ERROR_CODES.md` and `CONFIGURATION.md` are updated with the latest backend changes.
