package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(

        @NotBlank(message = "Current password cannot be blank")
        String currentPassword,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword) {
}
