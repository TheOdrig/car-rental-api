package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(

        @NotBlank(message = "Token cannot be blank")
        String token,

        @NotBlank(message = "New password cannot be blank")
        @Size(min = 8, max = 72, message = "New password must be between 8 and 72 characters")
        @MaxUtf8Bytes
        String newPassword) {
    @Override
    public String toString() {
        return "ResetPasswordRequest[token=[REDACTED], newPassword=[REDACTED]]";
    }
}
