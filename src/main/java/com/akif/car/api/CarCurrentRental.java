package com.akif.car.api;

public record CarCurrentRental(Long carId, Long rentalId, CarOccupancy occupancy) {
}
