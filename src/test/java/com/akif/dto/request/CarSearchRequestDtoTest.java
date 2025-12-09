package com.akif.dto.request;

import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import java.util.Set;

@DisplayName("CarSearchRequestDto Validation Tests")
public class CarSearchRequestDtoTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }


    @Test
    @DisplayName("Valid search request should pass validation")
    void validSearchRequest_ShouldPassValidation() {

        CarSearchRequestDto searchRequest = CarSearchRequestDto.builder()
                .searchTerm("Toyota")
                .brand("Toyota")
                .minProductionYear(2020)
                .maxProductionYear(2023)
                .minPrice(BigDecimal.valueOf(100000))
                .maxPrice(BigDecimal.valueOf(500000))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .sortBy("price")
                .sortDirection("asc")
                .page(0)
                .size(20)
                .build();

        Set<ConstraintViolation<CarSearchRequestDto>> violations = validator.validate(searchRequest);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Invalid sort field should fail validation")
    void invalidSortField_ShouldFailValidation() {

        CarSearchRequestDto searchRequest = CarSearchRequestDto.builder()
                .sortBy("invalidField")
                .build();

        Set<ConstraintViolation<CarSearchRequestDto>> violations = validator.validate(searchRequest);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Invalid sort field");
    }

    @Test
    @DisplayName("Invalid sort direction should fail validation")
    void invalidSortDirection_ShouldFailValidation() {

        CarSearchRequestDto searchRequest = CarSearchRequestDto.builder()
                .sortBy("price")
                .sortDirection("invalid")
                .build();

        Set<ConstraintViolation<CarSearchRequestDto>> violations = validator.validate(searchRequest);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Sort direction must be 'asc' or 'desc'");
    }

    @Test
    @DisplayName("Negative page should fail validation")
    void negativePage_ShouldFailValidation() {

        CarSearchRequestDto searchRequest = CarSearchRequestDto.builder()
                .page(-1)
                .build();

        Set<ConstraintViolation<CarSearchRequestDto>> violations = validator.validate(searchRequest);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Page number cannot be negative");
    }

    @Test
    @DisplayName("Invalid page size should fail validation")
    void invalidPageSize_ShouldFailValidation() {

        CarSearchRequestDto searchRequest = CarSearchRequestDto.builder()
                .size(101)
                .build();

        Set<ConstraintViolation<CarSearchRequestDto>> violations = validator.validate(searchRequest);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Page size cannot exceed 100");
    }








}
