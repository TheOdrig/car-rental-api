package com.akif.dashboard.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for processing return")
public record ReturnRequest(
        @Schema(description = "Notes about car condition at return", example = "Clean and full tank") String notes) {
}
