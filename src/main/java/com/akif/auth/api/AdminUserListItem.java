package com.akif.auth.api;

import com.akif.shared.enums.Role;

import java.time.LocalDateTime;
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
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt) {
    public String fullName() {
        if (firstName == null && lastName == null) {
            return email;
        }
        return ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
    }
}