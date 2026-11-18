package com.akif.car.internal.dto.request;

import com.akif.car.api.CarStatusType;
import jakarta.validation.constraints.NotNull;

public record CarStatusUpdateRequest(

    @NotNull(message = "Car status type cannot be null")
    CarStatusType carStatusType,

    String reason,
    String notes
) {}
