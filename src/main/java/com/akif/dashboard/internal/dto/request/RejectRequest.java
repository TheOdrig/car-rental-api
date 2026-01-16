package com.akif.dashboard.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request for rejecting a rental")
public record RejectRequest(
        @NotBlank(message = "Rejection reason is required") @Schema(description = "Reason for rejection", example = "Invalid driver license", requiredMode = Schema.RequiredMode.REQUIRED) String reason) {
}
