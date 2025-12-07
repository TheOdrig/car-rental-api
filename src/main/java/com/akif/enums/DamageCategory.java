package com.akif.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DamageCategory {
    
    SCRATCH("Scratch", "Surface scratches or paint damage"),
    DENT("Dent", "Body dents or deformations"),
    GLASS_DAMAGE("Glass Damage", "Windshield, windows, or mirror damage"),
    TIRE_DAMAGE("Tire Damage", "Tire puncture, wear, or rim damage"),
    INTERIOR_DAMAGE("Interior Damage", "Seats, dashboard, or interior components"),
    MECHANICAL_DAMAGE("Mechanical Damage", "Engine, transmission, or mechanical issues");
    
    private final String displayName;
    private final String description;

    public boolean requiresExtendedRepair() {
        return this == MECHANICAL_DAMAGE || this == GLASS_DAMAGE;
    }

    public boolean isExteriorDamage() {
        return this == SCRATCH || this == DENT || this == GLASS_DAMAGE || this == TIRE_DAMAGE;
    }
}
