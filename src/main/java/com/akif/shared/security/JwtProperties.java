package com.akif.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpiration,
        long refreshTokenExpiration,
        String issuer) {
    @Override
    public String toString() {
        return "JwtProperties[secret=[REDACTED], accessTokenExpiration=" + accessTokenExpiration + ", refreshTokenExpiration=" + refreshTokenExpiration + ", issuer=" + issuer + "]";
    }
}
