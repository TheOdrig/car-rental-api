package com.akif.notification.internal.service.email;

import com.akif.damage.api.*;
import com.akif.payment.api.PaymentCapturedEvent;
import com.akif.rental.api.*;

public interface IEmailTemplateService {

    String renderConfirmationEmail(RentalConfirmedEvent event);

    String renderPaymentReceiptEmail(PaymentCapturedEvent event);

    String renderPickupReminderEmail(PickupReminderEvent event);

    String renderReturnReminderEmail(ReturnReminderEvent event);

    String renderCancellationEmail(RentalCancelledEvent event);

    String renderGracePeriodWarningEmail(GracePeriodWarningEvent event);

    String renderLateReturnNotificationEmail(LateReturnNotificationEvent event);

    String renderSeverelyLateNotificationEmail(SeverelyLateNotificationEvent event);

    String renderPenaltySummaryEmail(PenaltySummaryEvent event);

    String renderDamageReportedEmail(DamageReportedEvent event);

    String renderDamageAssessedEmail(DamageAssessedEvent event);

    String renderDamageChargedEmail(DamageChargedEvent event);

    String renderDamageDisputedEmail(DamageDisputedEvent event);

    String renderDamageResolvedEmail(DamageResolvedEvent event);

    String renderPasswordResetEmail(String email, String resetLink);
}
