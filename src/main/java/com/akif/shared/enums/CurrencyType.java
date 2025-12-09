package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum CurrencyType {

    TRY("Turkish Lira", "₺", "TRY", 2, "tr", "TR"),
    USD("US Dollar", "$", "USD", 2, "en", "US"),
    EUR("Euro", "€", "EUR", 2, "en", "DE"),
    GBP("British Pound", "£", "GBP", 2, "en", "GB"),
    JPY("Japanese Yen", "¥", "JPY", 0, "ja", "JP");

    private final String fullName;
    private final String symbol;
    private final String code;
    private final int decimalPlaces;
    private final String languageCode;
    private final String countryCode;


    CurrencyType(String fullName, String symbol, String code, int decimalPlaces, String languageCode, String countryCode) {
        this.fullName = fullName;
        this.symbol = symbol;
        this.code = code;
        this.decimalPlaces = decimalPlaces;
        this.languageCode = languageCode;
        this.countryCode = countryCode;
    }


    @JsonValue
    public String getCode() {
        return code;
    }

    public Locale getLocale() {
        return new Locale(languageCode, countryCode);
    }


    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return symbol + "0." + "0".repeat(decimalPlaces);
        }

        BigDecimal roundedAmount = amount.setScale(decimalPlaces, RoundingMode.HALF_UP);
        return symbol + roundedAmount.toPlainString();
    }

    public String formatAmountWithSeparators(BigDecimal amount) {
        if (amount == null) {
            return symbol + "0." + "0".repeat(decimalPlaces);
        }

        Locale currencyLocale = this.getLocale();

        NumberFormat formatter = NumberFormat.getCurrencyInstance(currencyLocale);
        formatter.setMinimumFractionDigits(decimalPlaces);
        formatter.setMaximumFractionDigits(decimalPlaces);
        formatter.setRoundingMode(RoundingMode.HALF_UP);

        return formatter.format(amount);
    }


    public Currency getJavaCurrency(){
        return Currency.getInstance(code);
    }

    public boolean isMajorCurrency(){
        return this == USD || this == EUR || this == GBP || this == JPY;
    }


    public static CurrencyType fromString(String currency){
        if(currency==null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }

        String trimmed =  currency.trim().toUpperCase();

        for (CurrencyType type : values()){
            if(type.code.equals(trimmed) ||
               type.name().equals(trimmed) ||
               type.fullName.equalsIgnoreCase(trimmed)){
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown currency type: " + currency);
    }


    public static CurrencyType[] getMajorCurrencies(){
        return new CurrencyType[]{USD, EUR, GBP, JPY};
    }

    @Override
    public String toString(){
        return code;
    }
}
