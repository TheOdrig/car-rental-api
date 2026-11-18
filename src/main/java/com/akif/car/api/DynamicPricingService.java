package com.akif.car.api;

import java.time.LocalDate;

public interface DynamicPricingService {

    PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate);

    PricingResult previewPrice(Long carId, LocalDate startDate, LocalDate endDate);
}
