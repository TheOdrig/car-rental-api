package com.akif.payment.api;

public record PaymentRefundChangedEvent(Long paymentId, Long rentalId) {
}
