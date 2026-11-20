package com.akif.auth.internal.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(

        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @Size(max = 50, message = "Last name must not exceed 50 characters")
        String lastName,

        @Pattern(regexp = "^$|^\\+?[1-9]\\d{6,14}$", message = "Phone number must have 7 to 15 digits, with an optional leading +")
        String phone) {
}
