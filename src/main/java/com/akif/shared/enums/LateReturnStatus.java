package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum LateReturnStatus {
    ON_TIME("On Time", "Returned on or before end date"),
    GRACE_PERIOD("Grace Period", "Within grace period, no penalty"),
    LATE("Late", "Past grace period, penalty applies"),
    SEVERELY_LATE("Severely Late", "More than 24 hours late");

    private final String displayName;
    private final String description;

    LateReturnStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
