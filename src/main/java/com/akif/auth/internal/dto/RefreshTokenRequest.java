package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(

    @NotBlank(message = "Refresh token cannot be blank")
    String refreshToken
) {
    @Override
    public String toString() {
        return "RefreshTokenRequest[refreshToken=[REDACTED]]";
    }
}
