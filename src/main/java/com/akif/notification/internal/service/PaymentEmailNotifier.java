package com.akif.notification.internal.service;

import java.time.Clock;
import com.akif.damage.api.*;
import com.akif.notification.internal.exception.EmailSendException;
import com.akif.notification.internal.dto.EmailMessage;
import com.akif.notification.internal.service.email.EmailSender;
import com.akif.notification.internal.service.email.EmailTemplateService;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;
import com.akif.notification.domain.EmailType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEmailNotifier {

    private final Clock clock;
    private final EmailTemplateService templateService;
    private final EmailSender emailSender;

    public void sendPaymentReceipt(PaymentCapturedEvent event) {
        log.info("Queuing payment receipt email. PaymentId: {}, RentalId: {}",
                event.paymentId(), event.rentalId());

        String body = templateService.renderPaymentReceiptEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Payment Receipt - Transaction #" + event.transactionId(),
                body,
                EmailType.PAYMENT_RECEIPT,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Payment receipt email sent successfully. PaymentId: {}, Amount: {}, Timestamp: {}",
                    event.paymentId(), event.amount(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. PaymentId: {}, Error: {}, SMTP Code: {}",
                    event.paymentId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }

    public void sendPaymentRetryRequest(PaymentRetryRequestedEvent event) {
        log.info("Queuing payment retry email. RentalId: {}",
                event.rentalId());

        String body = templateService.renderPaymentRetryEmail(event);
        EmailMessage message = new EmailMessage(
                event.customerEmail(),
                "Payment needed - Rental #" + event.rentalId(),
                body,
                EmailType.PAYMENT_RETRY,
                event.rentalId());

        try {
            emailSender.send(message);
            log.info("Payment retry email sent successfully. RentalId: {}, Timestamp: {}",
                    event.rentalId(), Instant.now(clock));
        } catch (EmailSendException e) {
            log.warn("Email send attempt failed. RentalId: {}, Error: {}, SMTP Code: {}",
                    event.rentalId(), e.getMessage(), e.getSmtpErrorCode());
            throw e;
        }
    }
}
