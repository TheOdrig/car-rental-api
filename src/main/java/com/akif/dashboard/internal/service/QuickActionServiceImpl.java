package com.akif.dashboard.internal.service;

import com.akif.dashboard.api.QuickActionService;
import com.akif.dashboard.api.dto.DailySummaryDto;
import com.akif.dashboard.api.dto.QuickActionResultDto;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuickActionServiceImpl implements QuickActionService {

    private final RentalService rentalService;
    private final DashboardQueryService dashboardQueryService;

    @Override
    public QuickActionResultDto approveRental(Long rentalId) {
        log.info("Processing quick action: approve rental {}", rentalId);
        
        RentalResponse response = rentalService.confirmRental(rentalId);
        DailySummaryDto updatedSummary = dashboardQueryService.fetchDailySummary();
        
        log.info("Rental {} approved successfully, new status: {}", rentalId, response.status());
        
        return QuickActionResultDto.success(
            "Rental approved successfully",
            response.status().name(),
            updatedSummary
        );
    }

    @Override
    public QuickActionResultDto processPickup(Long rentalId) {
        log.info("Processing quick action: pickup rental {}", rentalId);
        
        RentalResponse response = rentalService.pickupRental(rentalId, "Processed via dashboard quick action");
        DailySummaryDto updatedSummary = dashboardQueryService.fetchDailySummary();
        
        log.info("Rental {} picked up successfully, new status: {}", rentalId, response.status());
        
        return QuickActionResultDto.success(
            "Pickup processed successfully",
            response.status().name(),
            updatedSummary
        );
    }

    @Override
    public QuickActionResultDto processReturn(Long rentalId) {
        log.info("Processing quick action: return rental {}", rentalId);
        
        RentalResponse response = rentalService.returnRental(rentalId, "Processed via dashboard quick action");
        DailySummaryDto updatedSummary = dashboardQueryService.fetchDailySummary();
        
        log.info("Rental {} returned successfully, new status: {}", rentalId, response.status());
        
        return QuickActionResultDto.success(
            "Return processed successfully",
            response.status().name(),
            updatedSummary
        );
    }
}
