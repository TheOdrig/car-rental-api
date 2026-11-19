package com.akif.contract;

import com.akif.car.api.AvailabilityStatus;
import com.akif.car.api.CarStatusType;
import com.akif.currency.api.RateSource;
import com.akif.damage.api.DamageCategory;
import com.akif.damage.api.DamageSeverity;
import com.akif.damage.api.DamageStatus;
import com.akif.payment.api.PaymentStatus;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Enums cross the wire as their stable constant names")
class EnumWireFormatTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final List<Class<? extends Enum<?>>> WIRE_ENUMS = List.of(
            AvailabilityStatus.class,
            CarStatusType.class,
            RateSource.class,
            PaymentStatus.class,
            RentalStatus.class,
            com.akif.rental.api.CancelledBy.class,
            LateReturnStatus.class,
            DamageStatus.class,
            DamageSeverity.class,
            DamageCategory.class,
            CurrencyType.class);

    @ParameterizedTest
    @ValueSource(strings = {"REQUESTED", "CONFIRMED", "IN_USE", "RETURNED", "CANCELLED"})
    @DisplayName("rental statuses serialise as codes and parse from codes")
    void rentalStatusCodes(String code) {
        RentalStatus status = MAPPER.readValue("\"" + code + "\"", RentalStatus.class);

        assertThat(status.name()).isEqualTo(code);
        assertThat(MAPPER.writeValueAsString(status)).isEqualTo("\"" + code + "\"");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("every wire enum round-trips through its constant name")
    void everyConstantRoundTrips() {
        for (Class<? extends Enum<?>> type : WIRE_ENUMS) {
            for (Enum<?> constant : type.getEnumConstants()) {
                String json = MAPPER.writeValueAsString(constant);

                assertThat(json).as(type.getSimpleName()).isEqualTo("\"" + constant.name() + "\"");
                assertThat((Object) MAPPER.readValue(json, type)).isSameAs(constant);
            }
        }
    }

    @org.junit.jupiter.api.Test
    @DisplayName("multi-word constants never use their display label")
    void labelsAreNotWireValues() {
        assertThat(MAPPER.writeValueAsString(RentalStatus.IN_USE)).isEqualTo("\"IN_USE\"");
        assertThat(MAPPER.writeValueAsString(LateReturnStatus.SEVERELY_LATE)).isEqualTo("\"SEVERELY_LATE\"");
        assertThat(MAPPER.writeValueAsString(DamageStatus.UNDER_ASSESSMENT)).isEqualTo("\"UNDER_ASSESSMENT\"");
    }
}
