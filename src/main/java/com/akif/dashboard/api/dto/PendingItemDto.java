package com.akif.dashboard.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PendingItemDto(
        Long rentalId,
        Long customerId,
        String customerName,
        String customerEmail,
        Long carId,
        String carBrand,
        String carModel,
        String licensePlate,
        String carImage,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal totalAmount,
        String status,
        Long lateHours,
        LocalDateTime createdAt) {
}
