package com.akif.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "oauth2")
@Getter
@Setter
public class OAuth2Properties {

    private ProviderConfig google = new ProviderConfig();
    private ProviderConfig github = new ProviderConfig();
    private StateConfig state = new StateConfig();

    @Getter
    @Setter
    public static class ProviderConfig {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
        private String authorizationUri;
        private String tokenUri;
        private String userInfoUri;
        private String userEmailsUri;
        private String scope;
    }


    @Getter
    @Setter
    public static class StateConfig {
        private String secret = "defaultSecretForDev";
        private int expirationMinutes = 10;
    }
}
