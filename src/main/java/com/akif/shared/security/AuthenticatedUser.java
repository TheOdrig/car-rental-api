package com.akif.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class AuthenticatedUser extends User {

    private final Long id;

    public AuthenticatedUser(Long id, String username, Collection<? extends GrantedAuthority> authorities) {
        super(username, "", authorities);
        this.id = id;
    }

    public Long id() {
        return id;
    }
}
