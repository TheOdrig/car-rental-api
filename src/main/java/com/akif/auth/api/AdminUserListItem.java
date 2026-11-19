package com.akif.auth.api;

import com.akif.shared.enums.Role;

import java.time.Instant;
import java.util.Set;


public record AdminUserListItem(
        Long id,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        String status,
        String avatarUrl,
        boolean isEmailVerified,
        Instant createdAt,
        Instant lastLoginAt) {
    public String fullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}