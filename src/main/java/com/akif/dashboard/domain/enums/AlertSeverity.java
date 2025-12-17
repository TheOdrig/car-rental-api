package com.akif.dashboard.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertSeverity {
    CRITICAL(1),
    HIGH(2),
    WARNING(3),
    MEDIUM(4),
    LOW(5);

    private final int priority;
}
