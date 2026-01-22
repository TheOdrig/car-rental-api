package com.akif.auth.internal.dto;

import jakarta.validation.constraints.Size;

public record UnbanUserRequest(
        @Size(max = 500, message = "Note cannot exceed 500 characters") String note) {
}
