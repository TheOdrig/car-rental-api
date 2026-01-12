# Requirements: Architecture Diagrams Documentation

## Overview

Bu spec, Car Rental API projesinin eksik UML diyagramlarını oluşturmayı hedefler. Mevcut Module Dependency Diagram'a ek olarak state, sequence ve component diyagramları eklenecek.

## Actors

- **Developer**: Sistem akışlarını görsel olarak anlayan geliştirici
- **Tech Lead**: Mimari tasarımı değerlendiren teknik lider
- **New Team Member**: Projeye yeni katılan ve akışları öğrenmek isteyen kişi

## User Stories

### Story 1: Rental Lifecycle State Diagram

**As a** developer  
**I want** to see the rental status transitions visually  
**So that** I can understand valid state changes and implement them correctly

**Acceptance Criteria:**
- State diagram `docs/architecture/DIAGRAMS.md` dosyasında oluşturulmuş
- Tüm RentalStatus değerleri gösterilmiş (REQUESTED, CONFIRMED, IN_USE, RETURNED, CANCELLED)
- Valid transitions oklar ile belirtilmiş
- Transition trigger'ları (confirm, pickup, return, cancel) etiketlenmiş
- canConfirm(), canPickup(), canReturn(), canCancel() metodları ile uyumlu

### Story 2: Payment Status State Diagram

**As a** developer  
**I want** to see the payment status transitions visually  
**So that** I can understand payment flow and error handling

**Acceptance Criteria:**
- State diagram `docs/architecture/DIAGRAMS.md` dosyasında oluşturulmuş
- Tüm PaymentStatus değerleri gösterilmiş (PENDING, AUTHORIZED, CAPTURED, FAILED, REFUNDED)
- Stripe webhook trigger'ları belirtilmiş
- Error path (FAILED) ve recovery path gösterilmiş
- canRefund() metodu ile uyumlu

### Story 3: Complete Rental Flow Sequence Diagram

**As a** developer  
**I want** to see the end-to-end rental flow  
**So that** I can understand module interactions during rental creation

**Acceptance Criteria:**
- Sequence diagram `docs/architecture/DIAGRAMS.md` dosyasında oluşturulmuş
- Actors: User, RentalController, RentalService, CarService, PaymentService, Stripe
- Request → Confirm → Payment Authorization akışı gösterilmiş
- Event publishing (RentalConfirmedEvent) gösterilmiş
- Error handling path gösterilmiş

### Story 4: Payment Webhook Flow Sequence Diagram

**As a** developer  
**I want** to see how Stripe webhooks are processed  
**So that** I can understand async payment confirmation flow

**Acceptance Criteria:**
- Sequence diagram `docs/architecture/DIAGRAMS.md` dosyasında oluşturulmuş
- Actors: Stripe, WebhookController, PaymentService, RentalService, NotificationService
- Webhook signature verification gösterilmiş
- Payment status update akışı gösterilmiş
- Event publishing (PaymentCompletedEvent) gösterilmiş

### Story 5: System Component Diagram

**As a** developer  
**I want** to see the high-level system architecture  
**So that** I can understand external integrations and boundaries

**Acceptance Criteria:**
- Component diagram `docs/architecture/DIAGRAMS.md` dosyasında oluşturulmuş
- Components: Frontend (placeholder), API, Database, External Services
- External services: Stripe, SendGrid, ExchangeRate-API, Google OAuth, GitHub OAuth
- Module boundaries gösterilmiş
- Data flow yönleri belirtilmiş

## Functional Requirements

### FR-1: Mermaid Format
- Tüm diyagramlar Mermaid formatında olmalı
- GitHub'da render edilebilir olmalı
- README'deki mevcut diyagram ile tutarlı stil

### FR-2: Single File Organization
- Tüm diyagramlar `docs/architecture/DIAGRAMS.md` dosyasında
- Table of contents ile organize
- Her diyagram için açıklama metni

### FR-3: Code Consistency
- State diagram'lar enum değerleri ile uyumlu
- Sequence diagram'lar gerçek service method isimleri ile uyumlu
- Transition'lar kod ile doğrulanabilir

## Non-Functional Requirements

### NFR-1: Readability
- Diyagramlar karmaşık olmamalı
- Renk kodlaması tutarlı olmalı
- Etiketler açık ve anlaşılır

### NFR-2: Maintainability
- Diyagramlar güncellenebilir yapıda
- Kod değişikliklerinde güncelleme kolay

## Out of Scope

- ER Diagram (database schema) - ayrı spec'te
- Class Diagram (entity relationships) - ayrı spec'te
- Deployment Diagram - operations spec'te
- README'deki mevcut Module Dependency Diagram güncellenmeyecek

## Dependencies

- Mevcut diyagram formatı: `README.md` (Module Dependency Diagram)
- RentalStatus enum: `com.akif.rental.domain.enums.RentalStatus`
- PaymentStatus enum: `com.akif.payment.api.PaymentStatus`
- Service implementations: `*ServiceImpl.java` dosyaları
