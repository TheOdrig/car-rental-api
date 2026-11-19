package com.akif.car.internal.dto.request;

import com.akif.car.api.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record CarRequest(
        @NotBlank(message = "licensePlate cannot be blank") @Size(min = 7, max = 11, message = "licensePlate must be 7 and 11 characters") @Pattern(regexp = "^[0-9]{2}[A-Z]{1,3}[0-9]{2,4}$", message = "licensePlate format is invalid") String licensePlate,
        @Size(min = 17, max = 17, message = "VIN number must be exactly 17 characters") @Pattern(regexp = "^[A-HJ-NPR-Z0-9]{17}$", message = "VIN number format is invalid") String vinNumber,
        @NotBlank(message = "Brand cannot be blank") @Size(max = 50, message = "Brand cannot exceed 50 characters") @Pattern(regexp = "^\\p{L}[\\p{L}\\s\\-]*$", message = "Brand can only contain letters, spaces and hyphens") String brand,
        @NotBlank(message = "Model cannot be blank") @Size(max = 50, message = "Model cannot exceed 50 characters") @Pattern(regexp = "^[a-zA-Z0-9\\s\\-]+$", message = "Model can only contain letters, numbers, spaces and hyphens") String model,
        @NotNull(message = "Production year cannot be null") @Min(value = 1900, message = "Production year must be at least 1900") @Max(value = 2030, message = "Production year cannot be in the future") Integer productionYear,
        @NotNull(message = "Price cannot be null") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive") @Digits(integer = 10, fraction = 2, message = "Price must have at most 10 integer digits and 2 decimal places") BigDecimal price,
        @NotNull(message = "Currency type cannot be null") CurrencyType currencyType,
        @NotNull(message = "Car status type cannot be null") CarStatusType carStatusType,
        @Size(max = 20, message = "Engine type cannot exceed 20 characters") @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Engine type can only contain letters and spaces") String engineType,
        @DecimalMin(value = "0.0", message = "Engine displacement cannot be negative") @DecimalMax(value = "10.0", message = "Engine displacement cannot exceed 10 liters") @Digits(integer = 2, fraction = 2, message = "Engine displacement must have at most 2 integer digits and 2 decimal places") BigDecimal engineDisplacement,
        @Size(max = 20, message = "Fuel type cannot exceed 20 characters") @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Fuel type can only contain letters and spaces") String fuelType,
        @Size(max = 20, message = "Transmission type cannot exceed 20 characters") @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Transmission type can only contain letters and spaces") String transmissionType,
        @Size(max = 20, message = "Body type cannot exceed 20 characters") @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Body type can only contain letters and spaces") String bodyType,
        @Size(max = 30, message = "Color cannot exceed 30 characters") @Pattern(regexp = "^[a-zA-Z\\s]*$", message = "Color can only contain letters and spaces") String color,
        @Min(value = 0, message = "Kilometer cannot be negative") @Max(value = 999999, message = "Kilometer cannot exceed 999,999 km") Long kilometer,
        @Min(value = 2, message = "Number of doors must be at least 2") @Max(value = 6, message = "Number of doors cannot exceed 6") Integer doors,
        @Min(value = 1, message = "Number of seats must be at least 1") @Max(value = 9, message = "Number of seats cannot exceed 9") Integer seats,
        @JsonFormat(pattern = "yyyy-MM-dd") @PastOrPresent(message = "Registration date cannot be in the future") LocalDate registrationDate,
        @JsonFormat(pattern = "yyyy-MM-dd") @PastOrPresent(message = "Last service date cannot be in the future") LocalDate lastServiceDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate nextServiceDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate insuranceExpiryDate,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate inspectionExpiryDate,
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters") String notes,
        @Size(max = 500, message = "Image URL cannot exceed 500 characters") @Pattern(regexp = "^(https?://).+$", message = "Image URL format is invalid") String imageUrl,
        @Size(max = 500, message = "Thumbnail URL cannot exceed 500 characters") @Pattern(regexp = "^(https?://).+$", message = "Thumbnail URL format is invalid") String thumbnailUrl,
        @DecimalMin(value = "1.0", message = "Rating must be at least 1.0") @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0") @Digits(integer = 1, fraction = 1, message = "Rating must have 1 decimal place") BigDecimal rating
) {

    public CarRequest normalized() {
        return toBuilder()
                .fuelType(canonical(fuelType))
                .transmissionType(canonical(transmissionType))
                .bodyType(canonical(bodyType))
                .build();
    }

    private static final java.util.Set<String> ACRONYMS = java.util.Set.of("suv", "mpv", "cvt", "lpg", "ev");

    private static String canonical(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\s+", " ");
        if (trimmed.isEmpty()) {
            return null;
        }
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        if (ACRONYMS.contains(lower)) {
            return lower.toUpperCase(java.util.Locale.ROOT);
        }
        StringBuilder result = new StringBuilder(lower.length());
        boolean startOfWord = true;
        for (char c : lower.toCharArray()) {
            result.append(startOfWord ? Character.toUpperCase(c) : c);
            startOfWord = !Character.isLetterOrDigit(c);
        }
        return result.toString();
    }
}
