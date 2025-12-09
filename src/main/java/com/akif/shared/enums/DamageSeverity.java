package com.akif.shared.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DamageSeverity {
    
    MINOR("Minor", "Small cosmetic damage"),
    MODERATE("Moderate", "Noticeable damage requiring repair"),
    MAJOR("Major", "Significant damage affecting functionality"),
    TOTAL_LOSS("Total Loss", "Vehicle severely damaged or totaled");
    
    private final String displayName;
    private final String description;

    public boolean requiresMaintenance() {
        return this == MAJOR || this == TOTAL_LOSS;
    }

    public boolean requiresAdminDecision() {
        return this == MODERATE;
    }
}

