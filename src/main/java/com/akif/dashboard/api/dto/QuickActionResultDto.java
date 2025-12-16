package com.akif.dashboard.api.dto;

public record QuickActionResultDto(
    boolean success,
    String message,
    String newStatus,
    DailySummaryDto updatedSummary
) {
    public static QuickActionResultDto success(String message, String newStatus, DailySummaryDto summary) {
        return new QuickActionResultDto(true, message, newStatus, summary);
    }
}
