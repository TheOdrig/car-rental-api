package com.akif.car.api;

import java.util.Map;

public interface CarCatalog {

    CarDto getCarDtoById(Long id);

    CarDto getCarForBooking(Long id);

    Map<Long, CarStatusType> findActiveCarStatuses();
}
