package com.akif.service.email;

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
}
