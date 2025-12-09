package com.akif.dto.request;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Set;

@DisplayName("CarRequestDto Validation Tests")
public class CarRequestDtoTest {

    private Validator validator;

    @BeforeEach
    public void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Test
    @DisplayName("Valid car request should pass validation")
    void validCarRequest_ShouldPassValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);
        
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Invalid license plate should fail validation")
    void invalidLicensePlate_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("INVALID")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("licensePlate format is invalid");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " "})
    @DisplayName("Blank license plate should fail validation")
    void blankLicensePlate_ShouldFailValidation(String licensePlate) {

        CarRequestDto carRequest = CarRequestDto.builder()
                .licensePlate(licensePlate)
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequest);

        assertThat(violations).hasSize(3);
        assertThat(violations)
                  .extracting(ConstraintViolation::getMessage)
                     .containsExactlyInAnyOrder(
                        "licensePlate cannot be blank",
                        "licensePlate must be 7 and 11 characters",
                        "licensePlate format is invalid"
                );
    }

    @Test
    @DisplayName("Invalid VIN number should fail validation")
    void invalidVinNumber_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .vinNumber("INVALID_VIN")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                    .containsExactlyInAnyOrder(
                            "VIN number must be exactly 17 characters",
                            "VIN number format is invalid"
                );
    }

    @Test
    @DisplayName("Blank brand should fail validation")
    void blankBrand_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand(" ")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Brand cannot be blank");
    }

    @Test
    @DisplayName("Future production year should fail validation")
    void futureProductionYear_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2035)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Production year cannot be in the future");
    }

    @Test
    @DisplayName("Negative price should fail validation")
    void negativePrice_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(-1000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Price must be positive");
    }

    @Test
    @DisplayName("Invalid image URL should fail validation")
    void invalidImageUrl_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .imageUrl("invalid-url")
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Image URL format is invalid");
    }

    @Test
    @DisplayName("Invalid rating should fail validation")
    void invalidRating_ShouldFailValidation() {

        CarRequestDto carRequestDto = CarRequestDto.builder()
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2023)
                .price(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .rating(BigDecimal.valueOf(6.0))
                .build();

        Set<ConstraintViolation<CarRequestDto>> violations = validator.validate(carRequestDto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Rating cannot exceed 5.0");
    }


}
