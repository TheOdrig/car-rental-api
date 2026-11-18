package com.akif.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class JwtUserAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

    public JwtUserAuthenticationConverter() {
        authoritiesConverter.setAuthoritiesClaimName(JwtTokenService.ROLES_CLAIM);
        authoritiesConverter.setAuthorityPrefix("ROLE_");
    }

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = authoritiesConverter.convert(jwt);
        User principal = new User(jwt.getSubject(), "", authorities);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, jwt, authorities);
        authentication.setDetails(jwt);
        return authentication;
    }
}
