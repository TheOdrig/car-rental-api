# Implementation Plan

## Email Notification System

- [x] 1. Set up project dependencies and configuration




  - [x] 1.1 Add required dependencies to pom.xml


    - Add spring-boot-starter-mail
    - Add spring-boot-starter-thymeleaf
    - Add spring-retry and spring-aspects
    - _Requirements: 7.1_
  - [x] 1.2 Create AsyncConfig with @EnableAsync and @EnableRetry


    - Configure emailTaskExecutor bean with core=2, max=10, queue=100
    - _Requirements: 6.1, 6.2_
  - [x] 1.3 Add email configuration properties


    - Add SMTP settings to application.properties
    - Add email.from and email.enabled properties
    - _Requirements: 7.1_

- [x] 2. Create domain events



  - [x] 2.1 Create base RentalEvent class


    - Extend ApplicationEvent
    - Include rentalId, customerEmail, occurredAt fields
    - _Requirements: 1.1_
  - [x] 2.2 Create RentalConfirmedEvent


    - Include carBrand, carModel, pickupDate, returnDate, totalPrice, currency, pickupLocation
    - _Requirements: 1.1_
  - [x] 2.3 Create PaymentCapturedEvent


    - Include paymentId, rentalId, customerEmail, amount, currency, transactionId, paymentDate
    - _Requirements: 2.1_
  - [x] 2.4 Create PickupReminderEvent


    - Include pickupDate, pickupLocation, carBrand, carModel
    - _Requirements: 3.2_
  - [x] 2.5 Create ReturnReminderEvent


    - Include returnDate, returnLocation, dailyPenaltyRate
    - _Requirements: 4.2_
  - [x] 2.6 Create RentalCancelledEvent


    - Include cancellationDate, cancellationReason, refundProcessed, refundAmount, refundTransactionId
    - _Requirements: 5.1_

- [x] 3. Create email infrastructure





  - [x] 3.1 Create EmailType enum


    - RENTAL_CONFIRMATION, PAYMENT_RECEIPT, PICKUP_REMINDER, RETURN_REMINDER, CANCELLATION_CONFIRMATION
    - _Requirements: 1.2, 2.2, 3.3, 4.3, 5.2_
  - [x] 3.2 Create EmailMessage record


    - Include to, subject, body, type, referenceId fields
    - _Requirements: 1.2_
  - [x] 3.3 Create EmailSendException


    - Include recipient, emailType, smtpErrorCode fields
    - _Requirements: 6.3_
  - [x] 3.4 Create IEmailSender interface


    - Define send(EmailMessage) method
    - _Requirements: 1.2_
  - [x] 3.5 Create SendGridEmailSender (production)


    - Implement IEmailSender with @Profile("prod")
    - Use JavaMailSender for SMTP
    - _Requirements: 7.3_



  - [x] 3.6 Create MockEmailSender (development)
    - Implement IEmailSender with @Profile("!prod")
    - Log email details instead of sending
    - _Requirements: 7.2_

- [ ] 4. Create email templates








  - [x] 4.1 Create rental-confirmation.html template

    - Include rental ID, car details, dates, total price, pickup location
    - _Requirements: 1.3_

  - [x] 4.2 Create payment-receipt.html template

    - Include transaction ID, amount, currency, payment date, rental reference
    - _Requirements: 2.3_
  - [x] 4.3 Create pickup-reminder.html template


    - Include pickup date, time window, location, car details, rental ID
    - _Requirements: 3.4_
  - [x] 4.4 Create return-reminder.html template


    - Include return date, location, late penalty info, rental ID
    - _Requirements: 4.4_
  - [x] 4.5 Create cancellation-confirmation.html template


    - Include rental ID, cancellation date, refund details if applicable
    - _Requirements: 5.3, 5.4_

- [x] 5. Create EmailTemplateService





  - [x] 5.1 Implement EmailTemplateService

    - Inject SpringTemplateEngine
    - Create render methods for each email type
    - _Requirements: 1.3, 2.3, 3.4, 4.4, 5.3_

- [x] 6. Create EmailNotificationService






  - [x] 6.1 Create IEmailNotificationService interface

    - Define methods for each email type
    - _Requirements: 1.2, 2.2, 3.3, 4.3, 5.2_

  - [x] 6.2 Implement EmailNotificationService

    - Inject EmailTemplateService and IEmailSender
    - Add @Retryable with maxAttempts=4, exponential backoff
    - Add @Recover for failure handling
    - Implement logging for all operations
    - _Requirements: 6.3, 6.4, 8.1, 8.2, 8.3_

- [x] 7. Create EmailEventListener




  - [x] 7.1 Implement EmailEventListener

    - Add @EventListener methods for each event type
    - Add @Async("emailTaskExecutor") for async processing
    - Delegate to EmailNotificationService
    - _Requirements: 1.2, 2.2, 3.3, 4.3, 5.2, 6.1_

- [x] 8. Checkpoint - Verify implementation compiles

  - Ensure application compiles and starts successfully
  - Ensure all tests pass, ask the user if questions arise.

- [x] 9. Integrate event publishing into RentalService





  - [x] 9.1 Add ApplicationEventPublisher to RentalServiceImpl


    - Inject ApplicationEventPublisher
    - _Requirements: 1.1_
  - [x] 9.2 Publish RentalConfirmedEvent in confirmRental


    - Create and publish event after successful confirmation
    - _Requirements: 1.1_
  - [x] 9.3 Publish RentalCancelledEvent in cancelRental


    - Create and publish event with refund details
    - _Requirements: 5.1_

- [x] 10. Integrate event publishing into payment flow



  - [x] 10.1 Publish PaymentCapturedEvent in pickupRental


    - Create and publish event after payment capture
    - _Requirements: 2.1_

- [x] 11. Create ReminderScheduler



  - [x] 11.1 Add reminder tracking fields to Rental entity


    - Add pickupReminderSent and returnReminderSent boolean fields
    - Create Flyway migration for new columns
    - _Requirements: 3.5_
  - [x] 11.2 Add reminder query methods to RentalRepository


    - findRentalsForPickupReminder(LocalDate tomorrow, RentalStatus status)
    - findRentalsForReturnReminder(LocalDate today, RentalStatus status)
    - _Requirements: 3.1, 4.1_
  - [x] 11.3 Implement ReminderScheduler


    - Add @Scheduled methods for pickup (8 AM) and return (9 AM) reminders
    - Query rentals and publish reminder events
    - Mark rentals as reminder-sent after publishing
    - _Requirements: 3.1, 3.2, 3.5, 4.1, 4.2_

- [x] 12. Write unit tests




  - [x] 12.1 Write unit tests for email senders


    - Test MockEmailSender logs correctly
    - Test SendGridEmailSender calls JavaMailSender
    - _Requirements: 7.2, 7.3_
  - [x] 12.2 Write unit tests for EmailTemplateService


    - Test each template renders with correct data
    - Test templates contain required fields
    - _Requirements: 1.3, 2.3, 3.4, 4.4, 5.3_
  - [x] 12.3 Write unit tests for EmailNotificationService


    - Test successful email sending
    - Test retry behavior on failure
    - Test recovery after max retries
    - _Requirements: 6.3, 6.4_
  - [x] 12.4 Write unit tests for EmailEventListener


    - Test each event type triggers correct service method
    - _Requirements: 1.2, 2.2, 3.3, 4.3, 5.2_
  - [x] 12.5 Update RentalServiceTest for event publishing


    - Verify events are published with correct data
    - _Requirements: 1.1, 5.1, 2.1_
  - [ ]*12.6 Write unit tests for ReminderScheduler
    - Test pickup reminder query returns correct rentals
    - Test return reminder query returns correct rentals
    - Test reminder-sent flag prevents duplicates
    - _Requirements: 3.1, 3.5, 4.1_

- [x] 13. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 14. Write integration tests





  - [x] 14.1 Create EmailEventListenerIntegrationTest
    - Test full event flow from publish to send
    - Use MockEmailSender to verify email content

    - _Requirements: 1.2, 2.2, 3.3, 4.3, 5.2_

  - [x] 14.2 Create ReminderSchedulerIntegrationTest



    - Test scheduled job execution with test data
    - Verify correct rentals are selected
    - _Requirements: 3.1, 4.1_

- [x] 15. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 16. Update README documentation





  - [x] 16.1 Add Email Notification section to README.md

    - Document email types (confirmation, receipt, reminders, cancellation)
    - Document configuration properties
    - Document SendGrid setup for production
    - Add example environment variables
    - _Requirements: 7.1, 7.2, 7.3_
