package com.akif.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OwnerOrAdmin")
class OwnerOrAdminTest {

    @Test
    @DisplayName("an admin is always permitted")
    void adminPermitted() {
        assertThat(OwnerOrAdmin.permits(true, 1L, 2L)).isTrue();
        assertThat(OwnerOrAdmin.permits(true, null, 2L)).isTrue();
    }

    @Test
    @DisplayName("the owner is permitted and everyone else is not")
    void ownerOnly() {
        assertThat(OwnerOrAdmin.permits(false, 7L, 7L)).isTrue();
        assertThat(OwnerOrAdmin.permits(false, 7L, 8L)).isFalse();
        assertThat(OwnerOrAdmin.permits(false, null, 8L)).isFalse();
        assertThat(OwnerOrAdmin.permits(false, null, null)).isFalse();
    }

    @Test
    @DisplayName("admin is read from the granted authorities")
    void adminFromAuthorities() {
        AuthenticatedUser admin = new AuthenticatedUser(1L, "a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        AuthenticatedUser user = new AuthenticatedUser(2L, "u", List.of(new SimpleGrantedAuthority("ROLE_USER")));

        assertThat(OwnerOrAdmin.isAdmin(admin)).isTrue();
        assertThat(OwnerOrAdmin.isAdmin(user)).isFalse();
        assertThat(OwnerOrAdmin.isAdmin(null)).isFalse();
    }
}
