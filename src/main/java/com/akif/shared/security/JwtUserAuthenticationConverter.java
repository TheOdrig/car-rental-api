package com.akif.shared.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
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
        AuthenticatedUser principal = new AuthenticatedUser(userId(jwt), jwt.getSubject(), authorities);
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(principal, jwt, authorities);
        authentication.setDetails(jwt);
        return authentication;
    }

    private static Long userId(Jwt jwt) {
        Object claim = jwt.getClaim(JwtTokenService.USER_ID_CLAIM);
        if (claim instanceof Number number) {
            return number.longValue();
        }
        throw new InvalidBearerTokenException("The token does not identify a user",
                new IllegalArgumentException("missing " + JwtTokenService.USER_ID_CLAIM + " claim"));
    }
}
