package com.akif.rental.api;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum CancelledBy {
    CUSTOMER,
    ADMIN,
    SYSTEM;

    @JsonValue
    public String code() {
        return name();
    }
}
