# Implementation Plan: Architecture Diagrams Documentation

## Overview

Bu plan, Car Rental API projesi için 5 kritik diyagramı tek bir markdown dosyasında oluşturmayı adım adım tanımlar. Tüm diyagramlar Mermaid formatında olacak.

## Tasks

- [x] 1. Create DIAGRAMS.md skeleton ✅
  - Create `docs/architecture/DIAGRAMS.md`
  - Add title and introduction
  - Add table of contents with anchor links
  - _Requirements: FR-2_

- [x] 2. Create Rental Lifecycle State Diagram ✅
  - [x] 2.1 Add section header and description
    - Explain rental lifecycle purpose
    - _Requirements: Story 1_
  - [x] 2.2 Add states table
    - List all RentalStatus enum values
    - Include displayName and description
    - _Requirements: Story 1 - All status values shown_
  - [x] 2.3 Add transitions table
    - Document valid transitions with triggers
    - Reference canConfirm(), canPickup(), etc.
    - _Requirements: Story 1 - Valid transitions_
  - [x] 2.4 Add Mermaid stateDiagram
    - REQUESTED → CONFIRMED → IN_USE → RETURNED
    - CANCELLED as terminal state from multiple sources
    - Add notes for key states
    - _Requirements: Story 1 - Transition triggers labeled_

- [x] 3. Create Payment Status State Diagram ✅
  - [x] 3.1 Add section header and description
    - Explain payment flow and Stripe integration
    - _Requirements: Story 2_
  - [x] 3.2 Add states table
    - List all PaymentStatus enum values
    - Include displayName and description
    - _Requirements: Story 2 - All status values shown_
  - [x] 3.3 Add transitions table
    - Document webhook triggers
    - Reference canRefund() method
    - _Requirements: Story 2 - Webhook triggers_
  - [x] 3.4 Add Mermaid stateDiagram
    - PENDING → AUTHORIZED → CAPTURED
    - FAILED as error state
    - REFUNDED from CAPTURED
    - _Requirements: Story 2 - Error and recovery paths_

- [x] 4. Checkpoint - Review State Diagrams ✅
  - Verify enum values match code
  - Ensure transitions are accurate
  - Ask user for review

- [x] 5. Create Complete Rental Flow Sequence Diagram ✅
  - [x] 5.1 Add section header and description
    - Explain two-phase rental flow (Request + Confirm)
    - _Requirements: Story 3_
  - [x] 5.2 Add participants table
    - RentalServiceImpl, AuthService, CarService, DynamicPricingService, PaymentService, Stripe, EventPublisher
    - _Requirements: Story 3 - Actors listed_
  - [x] 5.3 Add flow steps description
    - **Request Phase:** getUserByUsername → getCarById → status check → calculatePrice → save(REQUESTED)
    - **Confirm Phase:** authorize → createPayment → updatePaymentStatus → reserveCar → publish(RentalConfirmedEvent)
    - _Requirements: Story 3 - Request to Confirm flow_
  - [x] 5.4 Add Mermaid sequenceDiagram
    - Two phases with Note separators
    - Request: User → Controller → RentalServiceImpl → AuthService/CarService/DynamicPricingService
    - Confirm: Admin → Controller → RentalServiceImpl → PaymentService/Stripe → CarService → EventPublisher
    - _Requirements: Story 3 - Event publishing shown_

- [x] 6. Create Payment Webhook Flow Sequence Diagram ✅
  - [x] 6.1 Add section header and description
    - Explain Stripe Checkout webhook processing (NOT main rental flow)
    - Note: Business events are published by RentalServiceImpl, NOT webhook handler
    - _Requirements: Story 4_
  - [x] 6.2 Add participants table
    - Stripe, StripeWebhookController, StripeWebhookHandler, WebhookEventRepository, PaymentRepository
    - _Requirements: Story 4 - Actors listed_
  - [x] 6.3 Add supported event types
    - `checkout.session.completed` → CAPTURED
    - `checkout.session.expired` → FAILED
    - `payment_intent.payment_failed` → FAILED
  - [x] 6.4 Add Mermaid sequenceDiagram
    - Stripe → Controller → Handler → Repositories (NO PaymentService, NO EventPublisher)
    - Include signature verification (alt block)
    - Include idempotency check (isEventAlreadyProcessed)
    - Include WebhookEvent status tracking (PROCESSING → PROCESSED)
    - _Requirements: Story 4 - Signature verification_
  - [x] 6.5 Add Important Notes section
    - ⚠️ StripeWebhookHandler does NOT publish events
    - PaymentCapturedEvent/RentalConfirmedEvent are published by RentalServiceImpl

- [x] 7. Checkpoint - Review Sequence Diagrams ✅
  - Verify service method names match code
  - Ensure flow is accurate
  - Ask user for review

- [x] 8. Create System Component Diagram ✅
  - [x] 8.1 Add section header and description
    - Explain high-level architecture
    - _Requirements: Story 5_
  - [x] 8.2 Add components table
    - List all internal and external components
    - _Requirements: Story 5 - Components listed_
  - [x] 8.3 Add Mermaid flowchart
    - External Clients subgraph
    - API Modules subgraph
    - Data Layer subgraph
    - External Services subgraph
    - _Requirements: Story 5 - Module boundaries, External services_
  - [x] 8.4 Add data flow arrows
    - Client → API → Database
    - API → External Services
    - Webhooks (dotted lines)
    - _Requirements: Story 5 - Data flow directions_

- [x] 9. Add cross-references ✅
  - Link to README module diagram
  - Link to relevant ADRs
  - Add "See Also" section
  - _Requirements: FR-2_

- [x] 10. Final Checkpoint - Review all diagrams ✅
  - Verify all 5 stories' acceptance criteria met
  - Test Mermaid rendering in GitHub preview
  - Ensure consistency with existing documentation
  - Ask user for final review

## Notes

- Tüm diyagramlar Mermaid formatında (GitHub native rendering)
- README'deki mevcut Module Dependency Diagram referans alınacak
- Enum değerleri ve method isimleri koddan doğrulanacak
- Diyagramlar karmaşık olmamalı, okunabilirlik öncelikli
