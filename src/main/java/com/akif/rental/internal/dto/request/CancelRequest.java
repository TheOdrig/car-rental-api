package com.akif.rental.internal.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request for cancelling a rental")
public record CancelRequest(
        @Schema(description = "Reason for cancellation", example = "Customer requested cancellation") String reason) {
}
