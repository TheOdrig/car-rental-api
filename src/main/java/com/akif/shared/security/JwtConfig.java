package com.akif.shared.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    static final int MIN_SECRET_BYTES = 32;

    @Bean
    SecretKey jwtSigningKey(JwtProperties properties) {
        byte[] bytes = properties.secret() == null ? new byte[0] : properties.secret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("jwt.secret must be at least " + MIN_SECRET_BYTES + " bytes");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
    }

    @Bean
    @Primary
    JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties properties, Clock clock) {
        return decoder(jwtSigningKey, properties, clock);
    }

    private JwtDecoder decoder(SecretKey key, JwtProperties properties, Clock clock) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(properties.issuer()),
                tokenUseValidator()));
        return decoder;
    }

    private OAuth2TokenValidator<Jwt> tokenUseValidator() {
        return jwt -> TokenUse.ACCESS.claimValue().equals(jwt.getClaimAsString(TokenUse.CLAIM))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Token is not an access token", null));
    }
}
