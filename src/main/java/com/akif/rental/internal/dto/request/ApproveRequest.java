package com.akif.rental.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for approving a rental")
public record ApproveRequest(
        @Schema(description = "Optional notes for approval", example = "Approved for premium customer") String notes) {
}
