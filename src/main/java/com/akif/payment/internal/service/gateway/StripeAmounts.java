package com.akif.payment.internal.service.gateway;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;

public final class StripeAmounts {

    private StripeAmounts() {
    }

    public static long toMinorUnits(BigDecimal amount, CurrencyType currency) {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("Amount and currency are required");
        }
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        int places = currency.getDecimalPlaces();
        try {
            return amount.setScale(places, RoundingMode.UNNECESSARY).movePointRight(places).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount " + amount.toPlainString() + " has more precision than " + currency.name() + " allows", e);
        }
    }

    public static BigDecimal fromMinorUnits(long minor, CurrencyType currency) {
        int places = currency.getDecimalPlaces();
        return BigDecimal.valueOf(minor, places);
    }

    public static BigDecimal fromMinorUnits(long minor, String currencyCode) {
        int places;
        try {
            places = Math.max(0, Currency.getInstance(currencyCode.toUpperCase(Locale.ROOT)).getDefaultFractionDigits());
        } catch (IllegalArgumentException e) {
            places = 2;
        }
        return BigDecimal.valueOf(minor, places);
    }

    public static String code(CurrencyType currency) {
        return currency.name().toLowerCase(java.util.Locale.ROOT);
    }
}
