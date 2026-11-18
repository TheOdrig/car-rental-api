package com.akif.contract;

import com.akif.car.internal.dto.availability.AvailabilitySearchRequest;
import com.akif.car.internal.dto.pricing.PricingRequest;
import com.akif.rental.api.RentalRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Request validation helpers")
class RequestHelperVisibilityTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private static final LocalDate START = LocalDate.now().plusDays(10);
    private static final LocalDate END_BEFORE_START = START.minusDays(1);

    @Test
    @DisplayName("are not serialized to JSON")
    void notSerialized() throws Exception {
        JsonNode rental = mapper.readTree(mapper.writeValueAsString(
                new RentalRequest(1L, START, START.plusDays(2), null)));
        JsonNode pricing = mapper.readTree(mapper.writeValueAsString(
                new PricingRequest(1L, START, START.plusDays(2))));
        AvailabilitySearchRequest search = AvailabilitySearchRequest.builder()
                .startDate(START).endDate(START.plusDays(2)).build();
        JsonNode availability = mapper.readTree(mapper.writeValueAsString(search));

        assertThat(rental.propertyNames()).containsExactlyInAnyOrder("carId", "startDate", "endDate", "notes");
        assertThat(pricing.propertyNames()).containsExactlyInAnyOrder("carId", "startDate", "endDate");
        assertThat(availability.has("endDateAfterStartDate")).isFalse();
        assertThat(availability.has("maxPriceGreaterThanMinPrice")).isFalse();
        assertThat(availability.has("maxYearGreaterThanMinYear")).isFalse();
    }

    @Test
    @DisplayName("still run as validation rules, and rental dates are left to the domain policy")
    void stillValidated() {
        assertThat(violatedProperties(new RentalRequest(1L, START, END_BEFORE_START, null))).isEmpty();
        assertThat(violatedProperties(new RentalRequest(1L, LocalDate.now().minusDays(3), START.plusDays(40), null)))
                .isEmpty();
        assertThat(violatedProperties(new PricingRequest(1L, START, END_BEFORE_START)))
                .contains("endDateAfterStartDate");

        AvailabilitySearchRequest search = AvailabilitySearchRequest.builder()
                .startDate(START).endDate(END_BEFORE_START)
                .minPrice(new BigDecimal("100")).maxPrice(new BigDecimal("50"))
                .minProductionYear(2020).maxProductionYear(2019).build();
        assertThat(violatedProperties(search)).contains("endDateAfterStartDate",
                "maxPriceGreaterThanMinPrice", "maxYearGreaterThanMinYear");
    }

    private List<String> violatedProperties(Object target) {
        return validator.validate(target).stream()
                .map(ConstraintViolation::getPropertyPath).map(Object::toString).toList();
    }
}
