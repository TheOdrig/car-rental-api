package com.akif.rental.internal.service;

import java.math.BigDecimal;

public record RefundInfo(boolean refundProcessed, BigDecimal refundAmount, String refundTransactionId) {
}
