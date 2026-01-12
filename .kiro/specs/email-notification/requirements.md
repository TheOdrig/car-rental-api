# Requirements Document

## Introduction

This document specifies the requirements for implementing an event-driven email notification system in the rent-a-car application. The system will send automated emails to customers at key points in the rental lifecycle: reservation confirmation, payment receipt, pickup reminder, return reminder, and cancellation confirmation. The implementation uses Spring Events for decoupled event publishing and async processing for non-blocking email delivery.

## Glossary

- **Email_Notification_Service**: The service responsible for composing and sending emails to customers
- **Event_Publisher**: The component that publishes domain events when rental state changes occur
- **Event_Listener**: The component that listens for domain events and triggers email notifications
- **Email_Template**: An HTML template rendered with Thymeleaf containing dynamic content for each email type
- **SMTP**: Simple Mail Transfer Protocol used for sending emails
- **SendGrid**: A third-party email delivery service with SMTP relay and API options
- **Async Processing**: Non-blocking execution that allows the main thread to continue without waiting for email delivery
- **Domain Event**: An event representing something significant that happened in the business domain

## Requirements

### Requirement 1

**User Story:** As a customer, I want to receive an email confirmation when my rental request is confirmed, so that I have a record of my booking details.

#### Acceptance Criteria

1. WHEN a rental status changes to CONFIRMED THEN the Event_Publisher SHALL publish a RentalConfirmedEvent containing rental ID, customer email, car details, dates, and total price
2. WHEN a RentalConfirmedEvent is received THEN the Email_Notification_Service SHALL send a confirmation email to the customer within 30 seconds
3. WHEN composing the confirmation email THEN the Email_Notification_Service SHALL include rental ID, car brand and model, pickup date, return date, total price, and pickup location
4. WHEN the confirmation email is sent successfully THEN the Email_Notification_Service SHALL log the delivery status with rental ID and recipient email

### Requirement 2

**User Story:** As a customer, I want to receive a payment receipt email after my payment is captured, so that I have proof of payment for my records.

#### Acceptance Criteria

1. WHEN a payment status changes to CAPTURED THEN the Event_Publisher SHALL publish a PaymentCapturedEvent containing payment ID, rental ID, amount, currency, and transaction ID
2. WHEN a PaymentCapturedEvent is received THEN the Email_Notification_Service SHALL send a payment receipt email to the customer
3. WHEN composing the payment receipt email THEN the Email_Notification_Service SHALL include transaction ID, amount, currency, payment date, and rental reference number
4. WHEN the payment receipt email is sent successfully THEN the Email_Notification_Service SHALL log the delivery with payment ID and amount

### Requirement 3

**User Story:** As a customer, I want to receive a reminder email one day before my pickup date, so that I do not forget my scheduled rental.

#### Acceptance Criteria

1. WHEN the scheduled reminder job runs daily THEN the Reminder_Service SHALL identify all rentals with pickup date equal to tomorrow and status CONFIRMED
2. WHEN a rental requires a pickup reminder THEN the Reminder_Service SHALL publish a PickupReminderEvent containing rental ID and customer email
3. WHEN a PickupReminderEvent is received THEN the Email_Notification_Service SHALL send a pickup reminder email to the customer
4. WHEN composing the pickup reminder email THEN the Email_Notification_Service SHALL include pickup date, pickup time window, pickup location, car details, and rental ID
5. WHEN a pickup reminder is sent THEN the Reminder_Service SHALL mark the rental as reminder-sent to prevent duplicate reminders

### Requirement 4

**User Story:** As a customer, I want to receive a reminder email on my scheduled return date, so that I remember to return the car on time.

#### Acceptance Criteria

1. WHEN the scheduled reminder job runs daily THEN the Reminder_Service SHALL identify all rentals with return date equal to today and status IN_USE
2. WHEN a rental requires a return reminder THEN the Reminder_Service SHALL publish a ReturnReminderEvent containing rental ID and customer email
3. WHEN a ReturnReminderEvent is received THEN the Email_Notification_Service SHALL send a return reminder email to the customer
4. WHEN composing the return reminder email THEN the Email_Notification_Service SHALL include return date, return location, late return penalty information, and rental ID

### Requirement 5

**User Story:** As a customer, I want to receive a cancellation confirmation email when my rental is cancelled, so that I know the cancellation was processed.

#### Acceptance Criteria

1. WHEN a rental status changes to CANCELLED THEN the Event_Publisher SHALL publish a RentalCancelledEvent containing rental ID, customer email, cancellation reason, and refund status
2. WHEN a RentalCancelledEvent is received THEN the Email_Notification_Service SHALL send a cancellation confirmation email to the customer
3. WHEN composing the cancellation email THEN the Email_Notification_Service SHALL include rental ID, cancellation date, refund amount if applicable, and refund processing time estimate
4. IF a refund was processed THEN the cancellation email SHALL include the refund transaction ID and expected refund timeline

### Requirement 6

**User Story:** As a system operator, I want email sending to be asynchronous, so that email delivery does not block the main application flow.

#### Acceptance Criteria

1. WHEN an event listener receives a domain event THEN the Email_Notification_Service SHALL process the email asynchronously using a separate thread pool
2. WHEN async email processing is configured THEN the thread pool SHALL have a minimum of 2 threads and maximum of 10 threads
3. IF an email fails to send THEN the Email_Notification_Service SHALL retry up to 3 times with exponential backoff
4. IF all retry attempts fail THEN the Email_Notification_Service SHALL log the failure with event details and mark the notification as failed

### Requirement 7

**User Story:** As a system operator, I want configurable email settings, so that I can switch between development and production email providers.

#### Acceptance Criteria

1. WHEN the application starts THEN the Email_Configuration SHALL load SMTP settings from environment variables
2. WHEN running in development profile THEN the Email_Notification_Service SHALL use a mock email sender that logs emails instead of sending
3. WHEN running in production profile THEN the Email_Notification_Service SHALL use SendGrid SMTP relay for email delivery
4. WHEN email configuration is missing required values THEN the application SHALL fail fast with a clear error message

### Requirement 8

**User Story:** As a system operator, I want all email operations logged, so that I can troubleshoot delivery issues.

#### Acceptance Criteria

1. WHEN an email is queued for sending THEN the Email_Notification_Service SHALL log the email type, recipient, and rental ID
2. WHEN an email is sent successfully THEN the Email_Notification_Service SHALL log the delivery confirmation with timestamp
3. WHEN an email fails to send THEN the Email_Notification_Service SHALL log the error details including SMTP error code and message
4. WHEN a retry attempt is made THEN the Email_Notification_Service SHALL log the retry count and next retry time
