package com.akif.rental.api;

import java.time.Instant;

public interface RentalEvent {

    Long rentalId();

    String customerEmail();

    Instant occurredAt();
}
