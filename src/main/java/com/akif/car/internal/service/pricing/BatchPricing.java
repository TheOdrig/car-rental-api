package com.akif.car.internal.service.pricing;

import com.akif.car.api.PricingResult;
import com.akif.car.domain.Car;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

public interface BatchPricing {

    Map<Long, PricingResult> priceCars(Collection<Car> cars, LocalDate startDate, LocalDate endDate,
                                       LocalDate bookingDate);
}
