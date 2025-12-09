package com.akif.auth;

import com.akif.shared.enums.Role;

import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        String firstName,
        String lastName,
        Set<Role> roles,
        boolean active
) {
    public boolean isAdmin() {
        return roles != null && roles.contains(Role.ADMIN);
    }
}
