package com.akif.auth.api;

public record UserStatsResponse(
        long totalUsers,
        long activeUsers,
        long bannedUsers) {
}