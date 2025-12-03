package com.akif.event;

import com.akif.enums.CurrencyType;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PaymentCapturedEvent extends ApplicationEvent {
    
    private final Long paymentId;
    private final Long rentalId;
    private final String customerEmail;
    private final BigDecimal amount;
    private final CurrencyType currency;
    private final String transactionId;
    private final LocalDateTime paymentDate;
    
    public PaymentCapturedEvent(
            Object source,
            Long paymentId,
            Long rentalId,
            String customerEmail,
            BigDecimal amount,
            CurrencyType currency,
            String transactionId,
            LocalDateTime paymentDate) {
        super(source);
        this.paymentId = paymentId;
        this.rentalId = rentalId;
        this.customerEmail = customerEmail;
        this.amount = amount;
        this.currency = currency;
        this.transactionId = transactionId;
        this.paymentDate = paymentDate;
    }
}
