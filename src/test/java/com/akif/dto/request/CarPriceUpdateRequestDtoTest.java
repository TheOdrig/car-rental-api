package com.akif.dto.request;

import com.akif.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Set;

@DisplayName("CarPriceUpdateRequestDto Validation Tests")
public class CarPriceUpdateRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Test
    @DisplayName("Price with negative value should fail validation")
    void price_WithNegativeValue_ShouldFailValidation() {

        CarPriceUpdateRequestDto carPriceUpdateRequestDto = CarPriceUpdateRequestDto.builder()
                .price(BigDecimal.valueOf(-100))
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.valueOf(1000))
                .build();

        Set<ConstraintViolation<CarPriceUpdateRequestDto>> violations = validator.validate(carPriceUpdateRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Price must be positive");
    }

    @Test
    @DisplayName("Price with exceed digit constraint should fail validation")
    void price_WithDigitConstraint_ShouldFailValidation() {

        BigDecimal price = new BigDecimal("12345678901");

        CarPriceUpdateRequestDto carPriceUpdateRequestDto = CarPriceUpdateRequestDto.builder()
                .price(price)
                .currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.valueOf(1000))
                .build();

        Set<ConstraintViolation<CarPriceUpdateRequestDto>> violations = validator.validate(carPriceUpdateRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Price must have at most 10 integer digits and 2 decimal places");
    }

    @Test
    @DisplayName("Price with null should fail validation")
    void price_WithNull_ShouldFailValidation() {

        CarPriceUpdateRequestDto carPriceUpdateRequestDto = CarPriceUpdateRequestDto.builder()
                .price(null).
                currencyType(CurrencyType.TRY)
                .damagePrice(BigDecimal.valueOf(1000))
                .build();

        Set<ConstraintViolation<CarPriceUpdateRequestDto>> violations = validator.validate(carPriceUpdateRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Price cannot be null");
    }
}
