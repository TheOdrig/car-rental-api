# Implementation Plan: API Documentation

## Overview

Bu plan, Car Rental API projesi için 3 API dokümantasyon dosyası oluşturmayı adım adım tanımlar: ERROR_CODES.md, API_CONVENTIONS.md ve RATE_LIMITING.md.

## Tasks

- [x] 1. Create docs/api directory and ERROR_CODES.md skeleton
  - Create `docs/api/` directory
  - Create ERROR_CODES.md with title and overview
  - Add table of contents
  - _Requirements: Story 1_

- [x] 2. Document Error Response Format
  - [x] 2.1 Add error response JSON structure
    - timestamp, status, error, errorCode, message, path
    - _Requirements: Story 1 - Example response format_
  - [x] 2.2 Add cURL example for error response
    - Show how error looks in practice
    - _Requirements: FR-3_

- [x] 3. Document Auth Module Error Codes
  - Add Auth Module section header
  - Add error codes table (USER_ALREADY_EXISTS, INVALID_TOKEN, TOKEN_EXPIRED, OAUTH2_ERROR)
  - Include HTTP status, description, solution for each
  - _Requirements: Story 1 - Module grouping_

- [x] 4. Document Car Module Error Codes
  - Add Car Module section header
  - Add error codes table (CAR_NOT_FOUND, CAR_NOT_AVAILABLE, etc.)
  - Include HTTP status, description, solution for each
  - _Requirements: Story 1 - All error codes listed_

- [x] 5. Document Rental Module Error Codes
  - Add Rental Module section header
  - Add error codes table (RENTAL_NOT_FOUND, INVALID_RENTAL_STATE, etc.)
  - Include HTTP status, description, solution for each
  - _Requirements: Story 1 - All error codes listed_

- [x] 6. Document Payment, Damage, Dashboard Error Codes
  - [x] 6.1 Add Payment Module error codes
    - PAYMENT_FAILED, STRIPE_ERROR, WEBHOOK_SIGNATURE_INVALID
    - _Requirements: Story 1_
  - [x] 6.2 Add Damage Module error codes
    - DAMAGE_REPORT_ERROR, DAMAGE_ASSESSMENT_ERROR, DAMAGE_DISPUTE_ERROR
    - _Requirements: Story 1_
  - [x] 6.3 Add Dashboard Module error codes
    - ALERT_NOT_FOUND
    - _Requirements: Story 1_
  - [x] 6.4 Add Shared/Common error codes
    - INVALID_STATUS_TRANSITION, FILE_UPLOAD_ERROR
    - _Requirements: Story 1_

- [x] 7. Checkpoint - Review ERROR_CODES.md
  - Verify all exception classes are documented
  - Ensure HTTP status codes are accurate
  - Ask user for review

- [x] 8. Create API_CONVENTIONS.md
  - [x] 8.1 Create file with overview
    - Base URL, general conventions
    - _Requirements: Story 2_
  - [x] 8.2 Document Authentication section
    - Bearer token format
    - Header example
    - _Requirements: Story 2 - Auth header format_
  - [x] 8.3 Document Request/Response Format
    - Content-Type requirements
    - Accept header
    - _Requirements: Story 2 - Content-Type requirements_

- [x] 9. Document Pagination and Date Formats
  - [x] 9.1 Add Pagination section
    - page, size, sort parameters
    - Response structure with pageable object
    - _Requirements: Story 2 - Pagination format_
  - [x] 9.2 Add Date/Time Format section
    - ISO 8601 format
    - Date vs DateTime examples
    - Timezone (UTC)
    - _Requirements: Story 2 - Date/time format_

- [x] 10. Document Naming Conventions and Status Codes
  - [x] 10.1 Add Naming Conventions section
    - camelCase for JSON
    - kebab-case for URLs
    - UPPER_SNAKE for enums
    - _Requirements: Story 2 - Naming conventions_
  - [x] 10.2 Add HTTP Status Codes section
    - 2xx, 4xx, 5xx mappings
    - When each is used
    - _Requirements: Story 2 - Error response format_

- [x] 11. Checkpoint - Review API_CONVENTIONS.md
  - Verify conventions match actual API behavior
  - Ensure examples are accurate
  - Ask user for review

- [x] 12. Create RATE_LIMITING.md
  - [x] 12.1 Create file with overview
    - Rate limiting purpose
    - Current implementation status
    - _Requirements: Story 3_
  - [x] 12.2 Document Rate Limit Headers
    - X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset
    - Retry-After header
    - _Requirements: Story 3 - Rate limit headers_
  - [x] 12.3 Document 429 Response Handling
    - Response format
    - Retry strategy
    - _Requirements: Story 3 - 429 handling_
  - [x] 12.4 Add Best Practices section
    - Exponential backoff
    - Caching
    - Request queuing
    - _Requirements: Story 3 - Best practices_

- [x] 13. Final Checkpoint - Review all API documentation
  - Verify all 3 stories' acceptance criteria met
  - Ensure consistency across documents
  - Cross-reference with Swagger UI
  - Ask user for final review

## Notes

- Error codes mevcut exception sınıflarından alınacak
- HTTP status codes BaseException subclass'larından doğrulanacak
- Rate limiting mevcut durumu yansıtacak (implemented/planned)
- Örnekler gerçek API davranışı ile tutarlı olacak
