package com.akif.dashboard.api.dto;

import com.akif.dashboard.api.AlertSeverity;
import com.akif.dashboard.api.AlertType;

import java.time.LocalDateTime;

public record AlertDto(
    Long id,
    AlertType type,
    AlertSeverity severity,
    String title,
    String message,
    String actionUrl,
    boolean acknowledged,
    LocalDateTime acknowledgedAt,
    String acknowledgedBy,
    LocalDateTime createdAt
) {}

