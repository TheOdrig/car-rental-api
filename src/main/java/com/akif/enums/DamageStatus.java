package com.akif.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DamageStatus {
    
    REPORTED("Reported", "Damage reported, awaiting assessment"),
    UNDER_ASSESSMENT("Under Assessment", "Being evaluated by assessor"),
    ASSESSED("Assessed", "Assessment completed, awaiting charge"),
    CHARGED("Charged", "Customer charged for damage"),
    DISPUTED("Disputed", "Customer disputed the charge"),
    RESOLVED("Resolved", "Dispute resolved or damage closed");
    
    private final String displayName;
    private final String description;
    

    public boolean canBeAssessed() {
        return this == REPORTED || this == UNDER_ASSESSMENT;
    }

    public boolean canBeCharged() {
        return this == ASSESSED;
    }

    public boolean canBeDisputed() {
        return this == CHARGED;
    }

    public boolean canBeResolved() {
        return this == DISPUTED;
    }

    public boolean isTerminal() {
        return this == RESOLVED;
    }
}
