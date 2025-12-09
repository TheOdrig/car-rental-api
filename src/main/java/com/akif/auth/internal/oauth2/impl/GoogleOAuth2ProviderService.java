package com.akif.auth.internal.oauth2.impl;

import com.akif.config.OAuth2Properties;
import com.akif.auth.internal.oauth2.dto.response.OAuth2TokenResponse;
import com.akif.auth.internal.oauth2.dto.request.OAuth2UserInfo;
import com.akif.exception.OAuth2ProviderException;
import com.akif.auth.internal.oauth2.IOAuth2ProviderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
@Slf4j
public class GoogleOAuth2ProviderService implements IOAuth2ProviderService {

    private static final String PROVIDER_NAME = "google";

    private final OAuth2Properties oAuth2Properties;
    private final RestClient restClient;

    public GoogleOAuth2ProviderService(
            OAuth2Properties oAuth2Properties,
            @Qualifier("oAuth2RestClient") RestClient restClient) {
        this.oAuth2Properties = oAuth2Properties;
        this.restClient = restClient;
    }

    @Override
    public OAuth2TokenResponse exchangeCodeForTokens(String code) {
        OAuth2Properties.ProviderConfig config = oAuth2Properties.getGoogle();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", config.getRedirectUri());
        params.add("grant_type", "authorization_code");

        try {
            OAuth2TokenResponse response = restClient.post()
                    .uri(config.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(OAuth2TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                log.error("Google token exchange returned null or empty access token");
                throw OAuth2ProviderException.authenticationFailed(PROVIDER_NAME);
            }

            log.debug("Successfully exchanged code for tokens with Google");
            return response;
        } catch (RestClientException e) {
            log.error("Failed to exchange code for tokens with Google: {}", e.getMessage());
            throw OAuth2ProviderException.providerUnavailable(PROVIDER_NAME);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo getUserInfo(String accessToken) {
        OAuth2Properties.ProviderConfig config = oAuth2Properties.getGoogle();

        try {
            Map<String, Object> userAttributes = restClient.get()
                    .uri(config.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(Map.class);

            if (userAttributes == null) {
                log.error("Google user info response is null");
                throw OAuth2ProviderException.authenticationFailed(PROVIDER_NAME);
            }

            String providerId = (String) userAttributes.get("sub");
            String email = (String) userAttributes.get("email");
            String name = (String) userAttributes.get("name");
            String avatarUrl = (String) userAttributes.get("picture");

            if (email == null || email.isBlank()) {
                log.error("Google did not provide email for user");
                throw OAuth2ProviderException.emailRequired(PROVIDER_NAME);
            }

            log.debug("Successfully retrieved user info from Google for email: {}", email);
            return new OAuth2UserInfo(providerId, email, name, avatarUrl, PROVIDER_NAME);
        } catch (RestClientException e) {
            log.error("Failed to fetch user info from Google: {}", e.getMessage());
            throw OAuth2ProviderException.providerUnavailable(PROVIDER_NAME);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
