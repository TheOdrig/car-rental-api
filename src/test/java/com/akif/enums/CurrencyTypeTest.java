package com.akif.enums;

import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CurrencyType Enum Tests")
public class CurrencyTypeTest {

    @Test
    @DisplayName("getCode should return correct code")
    void getCode_ShouldReturnCorrectCode() {

        assertThat(CurrencyType.TRY.getCode()).isEqualTo("TRY");
        assertThat(CurrencyType.USD.getCode()).isEqualTo("USD");
        assertThat(CurrencyType.GBP.getCode()).isEqualTo("GBP");
    }

    @Test
    @DisplayName("getLocale should return correct Locale")
    void getLocale_ShouldReturnCorrectLocale() {

        assertThat(CurrencyType.TRY.getLocale().getCountry()).isEqualTo("TR");
        assertThat(CurrencyType.USD.getLocale().getCountry()).isEqualTo("US");
        assertThat(CurrencyType.GBP.getLocale().getCountry()).isEqualTo("GB");
    }


    @Test
    @DisplayName("formatAmount should format correctly with proper decimal places")
    void formatAmount_ShouldFormatCorrectly() {

        BigDecimal amount = new BigDecimal("1234.567");

        assertThat(CurrencyType.TRY.formatAmount(amount)).isEqualTo("₺1234.57");
        assertThat(CurrencyType.USD.formatAmount(amount)).isEqualTo("$1234.57");
        assertThat(CurrencyType.JPY.formatAmount(amount)).isEqualTo("¥1235");
    }

    @Test
    @DisplayName("formatAmount with null should return symbol with 0")
    void formatAmount_WithNull_ShouldReturnSymbolWithZero() {

        assertThat(CurrencyType.TRY.formatAmount(null)).isEqualTo("₺0.00");
        assertThat(CurrencyType.USD.formatAmount(null)).isEqualTo("$0.00");
        assertThat(CurrencyType.EUR.formatAmount(null)).isEqualTo("€0.00");
    }


    @Test
    @DisplayName("formatAmountWithSeparators should format with thousand separators")
    void formatAmountWithSeparators_ShouldFormatWithSeparators() {

        BigDecimal amount = new BigDecimal("1234567.89");

        assertThat(CurrencyType.TRY.formatAmountWithSeparators(amount)).isEqualTo("₺1.234.567,89");
        assertThat(CurrencyType.USD.formatAmountWithSeparators(amount)).isEqualTo("$1,234,567.89");
        assertThat(CurrencyType.GBP.formatAmountWithSeparators(amount)).isEqualTo("£1,234,567.89");
    }


    @Test
    @DisplayName("getJavaCurrency should return correct Currency object")
    void getJavaCurrency_ShouldReturnCorrectCurrency() {

        assertThat(CurrencyType.TRY.getJavaCurrency().getCurrencyCode()).isEqualTo("TRY");
        assertThat(CurrencyType.USD.getJavaCurrency().getCurrencyCode()).isEqualTo("USD");
        assertThat(CurrencyType.EUR.getJavaCurrency().getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("isMajorCurrency should return true for major currencies")
    void isMajorCurrency_ShouldReturnTrueForMajorCurrencies() {

        assertThat(CurrencyType.USD.isMajorCurrency()).isTrue();
        assertThat(CurrencyType.EUR.isMajorCurrency()).isTrue();
        assertThat(CurrencyType.GBP.isMajorCurrency()).isTrue();
        assertThat(CurrencyType.JPY.isMajorCurrency()).isTrue();
        assertThat(CurrencyType.TRY.isMajorCurrency()).isFalse();
    }


    @ParameterizedTest
    @ValueSource(strings = {"TRY", " try ", " Turkish Lira", "USD", " usd ", " US Dollar"})
    @DisplayName("fromString should parse valid currency strings")
    void fromString_WithValidStrings_ShouldReturnCorrectEnum(String currencyString) {

        CurrencyType result = CurrencyType.fromString(currencyString);

        assertThat(result).isNotNull();
        assertThat(result.getCode().equalsIgnoreCase(currencyString.trim()) ||
                result.name().equalsIgnoreCase(currencyString.trim()) ||
                result.getFullName().equalsIgnoreCase(currencyString.trim()))
                .isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("fromString should throw exception for invalid strings")
    void fromString_WithInvalidStrings_ShouldThrowException(String invalidString) {

        assertThatThrownBy(() -> CurrencyType.fromString(invalidString))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency code cannot be null or empty");
    }

    @Test
    @DisplayName("fromString with null should throw exception")
    void fromString_WithNull_ShouldThrowException() {

        assertThatThrownBy(() -> CurrencyType.fromString(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Currency code cannot be null or empty");
    }


    @Test
    @DisplayName("getMajorCurrencies should return correct currencies")
    void getMajorCurrencies_ShouldReturnCorrectCurrencies() {

        CurrencyType[] majorCurrencies = CurrencyType.getMajorCurrencies();

        assertThat(majorCurrencies).hasSize(4);
        assertThat(majorCurrencies).containsExactly(
                CurrencyType.USD,
                CurrencyType.EUR,
                CurrencyType.GBP,
                CurrencyType.JPY);
    }

    @Test
    @DisplayName("toString should return currency code")
    void toString_ShouldReturnCurrencyCode() {

        assertThat(CurrencyType.TRY.toString()).isEqualTo("TRY");
        assertThat(CurrencyType.USD.toString()).isEqualTo("USD");
        assertThat(CurrencyType.EUR.toString()).isEqualTo("EUR");
    }
}
