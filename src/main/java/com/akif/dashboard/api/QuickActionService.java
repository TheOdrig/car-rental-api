package com.akif.dashboard.api;

import com.akif.dashboard.api.dto.QuickActionResultDto;

public interface QuickActionService {

    QuickActionResultDto approveRental(Long rentalId, String notes);

    QuickActionResultDto rejectRental(Long rentalId, String reason, String username);

    QuickActionResultDto processPickup(Long rentalId, String notes);

    QuickActionResultDto processReturn(Long rentalId, String notes);
}
