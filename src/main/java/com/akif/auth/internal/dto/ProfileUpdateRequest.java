package com.akif.auth.internal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(

        @NotBlank(message = "First name cannot be blank")
        @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
        String firstName,

        @NotBlank(message = "Last name cannot be blank")
        @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
        String lastName,

        @Pattern(regexp = "^$|^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format (E.164)")
        String phone) {
}
