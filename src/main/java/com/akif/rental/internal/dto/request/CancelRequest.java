package com.akif.rental.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for cancelling a rental")
public record CancelRequest(
        @Size(max = 500, message = "Reason cannot exceed 500 characters")
        @Schema(description = "Optional reason for the cancellation", example = "Customer requested cancellation") String reason) {
}
