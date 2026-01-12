package com.akif.car.api;

import java.util.List;

public record FilterOptionsResponse(
    List<String> brands,
    List<String> transmissionTypes,
    List<String> fuelTypes,
    List<String> bodyTypes
) {}
