package com.akif.auth.api;

import com.akif.shared.enums.Role;

import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        String avatarUrl,
        Set<Role> roles,
        boolean active,
        boolean emailVerified) {

    public UserDto(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String phone,
            String avatarUrl,
            Set<Role> roles,
            boolean active) {
        this(id, username, email, firstName, lastName, phone, avatarUrl, roles, active, false);
    }

    public boolean isAdmin() {
        return roles != null && roles.contains(Role.ADMIN);
    }
}
