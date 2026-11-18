package com.akif.car.internal.service.pricing;

import java.util.List;

public interface PricingStrategyCatalog {

    List<PricingStrategy> getEnabledStrategies();
}
