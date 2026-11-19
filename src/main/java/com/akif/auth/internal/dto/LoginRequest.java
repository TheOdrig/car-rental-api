package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    String username,

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    @MaxUtf8Bytes
    String password
) {
    @Override
    public String toString() {
        return "LoginRequest[username=" + username + ", password=[REDACTED]]";
    }
}
