package com.akif.payment.domain.enums;

public enum DiscrepancyType {
    MISSING_IN_STRIPE,
    MISSING_IN_DATABASE,
    AMOUNT_MISMATCH,
    STATUS_MISMATCH,
    LOOKUP_FAILED
}
