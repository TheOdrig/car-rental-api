package com.akif.shared.enums;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum RateSource {

    LIVE("Live", "Real-time rates from external API"),
    CACHED("Cached", "Rates from cache (originally from API)"),
    FALLBACK("Fallback", "Static fallback rates (API unavailable)");

    private final String displayName;
    private final String description;

    RateSource(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public boolean isLive() {
        return this == LIVE;
    }

    public boolean isFallback() {
        return this == FALLBACK;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
