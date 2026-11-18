package com.akif.payment.unit;

import com.akif.payment.api.PaymentIntentResult;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.api.PaymentSummary;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Payment status serialization")
class PaymentStatusSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("PaymentSummary.status serializes as the enum name, never the display name")
    void summaryStatusIsEnumName() {
        PaymentSummary summary = new PaymentSummary(1L, 2L, PaymentStatus.AUTHORIZED.name(),
                BigDecimal.TEN, CurrencyType.USD, null, null);

        assertThat(mapper.writeValueAsString(summary)).contains("\"status\":\"AUTHORIZED\"").doesNotContain("Authorized");
    }

    @Test
    @DisplayName("PaymentIntentResult.status serializes as the enum name, never the display name")
    void intentStatusIsEnumName() {
        PaymentIntentResult result = new PaymentIntentResult(1L, 2L, "pi_1", "secret", null,
                PaymentStatus.CAPTURED.name(), BigDecimal.TEN, CurrencyType.USD);

        assertThat(mapper.writeValueAsString(result)).contains("\"status\":\"CAPTURED\"").doesNotContain("Captured");
    }
}
