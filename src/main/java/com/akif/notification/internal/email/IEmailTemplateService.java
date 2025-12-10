package com.akif.notification.internal.email;

import com.akif.event.*;

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
}

