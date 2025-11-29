package com.akif.service.pricing;

import java.time.LocalDate;
import java.util.List;

public interface IDynamicPricingService {

    PricingResult calculatePrice(Long carId, LocalDate startDate, LocalDate endDate, LocalDate bookingDate);

    PricingResult previewPrice(Long carId, LocalDate startDate, LocalDate endDate);

    List<PricingStrategy> getEnabledStrategies();
}
