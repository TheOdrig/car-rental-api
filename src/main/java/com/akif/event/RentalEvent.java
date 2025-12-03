package com.akif.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

@Getter
public abstract class RentalEvent extends ApplicationEvent {
    
    private final Long rentalId;
    private final String customerEmail;
    private final LocalDateTime occurredAt;
    
    protected RentalEvent(Object source, Long rentalId, String customerEmail, LocalDateTime occurredAt) {
        super(source);
        this.rentalId = rentalId;
        this.customerEmail = customerEmail;
        this.occurredAt = occurredAt;
    }
}
