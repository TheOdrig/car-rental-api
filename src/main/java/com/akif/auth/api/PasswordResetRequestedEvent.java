package com.akif.auth.api;

public record PasswordResetRequestedEvent(
        String email,
        String resetLink) {
}
