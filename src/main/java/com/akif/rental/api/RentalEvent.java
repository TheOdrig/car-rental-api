package com.akif.rental.api;

import java.time.LocalDateTime;

public interface RentalEvent {

    Long rentalId();

    String customerEmail();

    LocalDateTime occurredAt();
}
