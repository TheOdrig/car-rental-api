package com.akif.auth.api;

import com.akif.shared.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public record AdminUserDetailResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String avatarUrl,
        Set<Role> roles,
        LocalDateTime registrationDate,
        LocalDateTime lastLoginDate,
        VerificationInfo verification,
        UserStatistics statistics,
        AccountStatus accountStatus,
        List<AdminNoteDto> adminNotes) {

    public record VerificationInfo(
            boolean emailVerified,
            boolean phoneVerified,
            String documentsVerified,
            List<String> documentTypes) {
    }

    public record UserStatistics(
            int totalRentals,
            int completedRentals,
            int cancelledRentals,
            int activeRentals,
            BigDecimal totalSpent,
            double averageRentalDuration,
            int totalDamageReports,
            @Schema(description = "Customer damage liability grouped by assessment currency; amounts of different currencies are never summed together.")
            List<UserDamageStatisticsDto.CurrencyTotals> damageCostByCurrency,
            int lateReturns,
            int customerSinceDays) {
    }

    public record AccountStatus(
            String status,
            LocalDateTime bannedAt,
            String banReason,
            Long bannedBy,
            LocalDateTime lastStatusChange) {
    }
}
