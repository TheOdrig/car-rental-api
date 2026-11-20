package com.akif.shared.security;

import org.springframework.security.core.GrantedAuthority;

public final class OwnerOrAdmin {

    private static final String ADMIN_AUTHORITY = "ROLE_ADMIN";

    private OwnerOrAdmin() {
    }

    public static boolean permits(boolean admin, Long ownerId, Long callerId) {
        return admin || (ownerId != null && ownerId.equals(callerId));
    }

    public static boolean isAdmin(AuthenticatedUser caller) {
        if (caller == null) {
            return false;
        }
        for (GrantedAuthority authority : caller.getAuthorities()) {
            if (ADMIN_AUTHORITY.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
