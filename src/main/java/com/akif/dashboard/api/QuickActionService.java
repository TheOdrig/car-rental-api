package com.akif.dashboard.api;

import com.akif.dashboard.api.dto.QuickActionResultDto;

public interface QuickActionService {

    QuickActionResultDto approveRental(Long rentalId);

    QuickActionResultDto processPickup(Long rentalId);

    QuickActionResultDto processReturn(Long rentalId);
}
