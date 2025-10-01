package com.akif.dto.request;

import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CarRequestDto implements Serializable {


    @NotBlank(message = "licensePlate cannot be blank")
    @Size(min = 7, max = 11, message = "licensePlate must be 7 and 11 characters")
    @Pattern(regexp = "^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$", message = "licensePlate format is invalid")
    private String licensePlate;

    @Size(min = 17, max = 17, message = "VIN number must be exactly 17 characters")
    @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN number format is invalid")
    private String vinNumber;

    @NotBlank(message = "Brand cannot be blank")
    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Brand can only contain letters and spaces")
    private String brand;

    @NotBlank(message = "Model cannot be blank")
    @Size(max = 50, message = "Model cannot exceed 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9\\s\\-]+$", message = "Model can only contain letters, numbers, spaces and hyphens")
    private String model;

    @NotNull(message = "Production year cannot be null")
    @Min(value = 1900, message = "Production year must be at least 1900")
    @Max(value = 2030, message = "Production year cannot be in the future")
    private Integer productionYear;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
    @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Currency type cannot be null")
    private CurrencyType currencyType;

    @DecimalMin(value = "0.0", message = "Damage price cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Damage price must have at most 10 integer digits and 2 decimal places")
    private BigDecimal damagePrice = BigDecimal.ZERO;

    @NotNull(message = "Car status type cannot be null")
    private CarStatusType carStatusType;

    @Size(max = 20, message = "Engine type cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Engine type can only contain letters and spaces")
    private String engineType;

    @DecimalMin(value = "0.0", message = "Engine displacement cannot be negative")
    @DecimalMax(value = "10.0", message = "Engine displacement cannot exceed 10 liters")
    @Digits(integer = 2, fraction = 2, message = "Engine displacement must have at most 2 integer digits and 2 decimal places")
    private BigDecimal engineDisplacement;

    @Size(max = 20, message = "Fuel type cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Fuel type can only contain letters and spaces")
    private String fuelType;

    @Size(max = 20, message = "Transmission type cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Transmission type can only contain letters and spaces")
    private String transmissionType;

    @Size(max = 20, message = "Body type cannot exceed 20 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Body type can only contain letters and spaces")
    private String bodyType;

    @Size(max = 30, message = "Color cannot exceed 30 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Color can only contain letters and spaces")
    private String color;

    @Min(value = 0, message = "Kilometer cannot be negative")
    @Max(value = 999999, message = "Kilometer cannot exceed 999,999 km")
    private Long kilometer;

    @Min(value = 2, message = "Number of doors must be at least 2")
    @Max(value = 6, message = "Number of doors cannot exceed 6")
    private Integer doors;

    @Min(value = 1, message = "Number of seats must be at least 1")
    @Max(value = 9, message = "Number of seats cannot exceed 9")
    private Integer seats;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @PastOrPresent(message = "Registration date cannot be in the future")
    private LocalDate registrationDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    @PastOrPresent(message = "Last service date cannot be in the future")
    private LocalDate lastServiceDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate nextServiceDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate insuranceExpiryDate;

    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate inspectionExpiryDate;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
    private String notes;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
            message = "Image URL format is invalid")
    private String imageUrl;

    @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters")
    @Pattern(regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})([/\\w .-]*)*/?$",
            message = "Thumbnail URL format is invalid")
    private String thumbnailUrl;

    @Builder.Default
    private Boolean isFeatured = false;

    @Builder.Default
    private Boolean isTestDriveAvailable = true;

    @DecimalMin(value = "1.0", message = "Rating must be at least 1.0")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @Digits(integer = 1, fraction = 1, message = "Rating must have 1 decimal place")
    private BigDecimal rating;
}