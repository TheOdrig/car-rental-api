# Requirements: Project Basics Documentation

## Overview

Bu spec, Car Rental API projesinin eksik temel proje dosyalarını oluşturmayı hedefler. CONTRIBUTING.md ve CHANGELOG.md dosyaları eklenecek.

## Actors

- **Contributor**: Projeye katkıda bulunmak isteyen developer
- **Maintainer**: Projeyi yöneten ve PR'ları review eden kişi
- **User**: Projenin versiyon geçmişini takip eden kullanıcı

## User Stories

### Story 1: Contributing Guidelines

**As a** contributor  
**I want** to understand how to contribute to this project  
**So that** I can submit quality pull requests that follow project standards

**Acceptance Criteria:**
- `CONTRIBUTING.md` dosyası root dizinde oluşturulmuş
- Development environment setup açıklanmış
- Code style guidelines belirtilmiş (mevcut steering rules referans)
- Pull request process açıklanmış
- Commit message format belirtilmiş (Conventional Commits)
- Testing requirements belirtilmiş
- Issue reporting guidelines eklenmiş
- Code review process açıklanmış

### Story 2: Changelog Documentation

**As a** user  
**I want** to see the version history and changes  
**So that** I can understand what changed between versions

**Acceptance Criteria:**
- `CHANGELOG.md` dosyası root dizinde oluşturulmuş
- Semantic Versioning (SemVer) formatı kullanılmış
- Keep a Changelog formatı takip edilmiş
- Sections: Added, Changed, Deprecated, Removed, Fixed, Security
- Unreleased section mevcut
- v1.0.0 release (2025-11-28): Layered architecture, Railway deploy
- v2.0.0 release (2025-12-14): Spring Modulith migration (BREAKING CHANGE)
- Link format for version comparison

## Functional Requirements

### FR-1: Standard Formats
- CONTRIBUTING.md GitHub standartlarına uygun
- CHANGELOG.md Keep a Changelog formatında
- Semantic Versioning kullanılmalı

### FR-2: Project-Specific Content
- Mevcut test komutları referans verilmeli
- Mevcut CI/CD pipeline referans verilmeli
- Spring Modulith specific guidelines

### FR-3: Actionable Instructions
- Step-by-step setup instructions
- Clear PR checklist
- Concrete examples

## Non-Functional Requirements

### NFR-1: Discoverability
- GitHub otomatik olarak CONTRIBUTING.md'yi tanımalı
- README'den link verilmeli

### NFR-2: Maintainability
- Template formatı kolay güncellenebilir
- Version entries kolay eklenebilir

## Out of Scope

- CODE_OF_CONDUCT.md (opsiyonel, bu spec'te değil)
- SECURITY.md (security-documentation spec'te)
- Issue/PR templates (.github/ dizini)

## Dependencies

- Mevcut README.md
- Mevcut test komutları (mvn test)
- Mevcut CI/CD (modulith-verify.yml)
- Steering rules (.kiro/steering/)
