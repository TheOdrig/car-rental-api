package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddNoteRequest(
        @NotBlank(message = "Note text is required") @Size(max = 1000, message = "Note cannot exceed 1000 characters") String text) {
}
