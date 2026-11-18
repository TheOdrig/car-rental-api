package com.akif.car.api;

public record CarSummaryResponse(
    Long id,
    String licensePlate,
    String brand,
    String model,
    String thumbnailUrl
) {}
