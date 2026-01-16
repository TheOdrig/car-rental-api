package com.akif.dashboard.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for processing pickup")
public record PickupRequest(
        @Schema(description = "Notes about car condition at pickup", example = "Front left tire slightly worn") String notes) {
}
