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

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GitHubOAuth2ProviderService implements IOAuth2ProviderService {

    private static final String PROVIDER_NAME = "github";

    private final OAuth2Properties oAuth2Properties;
    private final RestClient restClient;

    public GitHubOAuth2ProviderService(
            OAuth2Properties oAuth2Properties,
            @Qualifier("oAuth2RestClient") RestClient restClient) {
        this.oAuth2Properties = oAuth2Properties;
        this.restClient = restClient;
    }

    @Override
    public OAuth2TokenResponse exchangeCodeForTokens(String code) {
        OAuth2Properties.ProviderConfig config = oAuth2Properties.getGithub();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", config.getRedirectUri());

        try {
            OAuth2TokenResponse response = restClient.post()
                    .uri(config.getTokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(params)
                    .retrieve()
                    .body(OAuth2TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                log.error("GitHub token exchange returned null or empty access token");
                throw OAuth2ProviderException.authenticationFailed(PROVIDER_NAME);
            }

            log.debug("Successfully exchanged code for tokens with GitHub");
            return response;
        } catch (RestClientException e) {
            log.error("Failed to exchange code for tokens with GitHub: {}", e.getMessage());
            throw OAuth2ProviderException.providerUnavailable(PROVIDER_NAME);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public OAuth2UserInfo getUserInfo(String accessToken) {
        OAuth2Properties.ProviderConfig config = oAuth2Properties.getGithub();

        try {
            Map<String, Object> userAttributes = restClient.get()
                    .uri(config.getUserInfoUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);

            if (userAttributes == null) {
                log.error("GitHub user info response is null");
                throw OAuth2ProviderException.authenticationFailed(PROVIDER_NAME);
            }

            Object idObj = userAttributes.get("id");
            String providerId = idObj != null ? String.valueOf(idObj) : null;
            String email = (String) userAttributes.get("email");
            String name = (String) userAttributes.get("name");
            String avatarUrl = (String) userAttributes.get("avatar_url");

            if (name == null || name.isBlank()) {
                name = (String) userAttributes.get("login");
            }

            if (email == null || email.isBlank()) {
                email = fetchPrimaryEmail(accessToken, config);
            }

            if (email == null || email.isBlank()) {
                log.error("GitHub did not provide email for user");
                throw OAuth2ProviderException.emailRequired(PROVIDER_NAME);
            }

            log.debug("Successfully retrieved user info from GitHub for email: {}", email);
            return new OAuth2UserInfo(providerId, email, name, avatarUrl, PROVIDER_NAME);
        } catch (OAuth2ProviderException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("Failed to fetch user info from GitHub: {}", e.getMessage());
            throw OAuth2ProviderException.providerUnavailable(PROVIDER_NAME);
        }
    }


    @SuppressWarnings("unchecked")
    private String fetchPrimaryEmail(String accessToken, OAuth2Properties.ProviderConfig config) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(config.getUserEmailsUri())
                    .header("Authorization", "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(List.class);

            if (emails == null || emails.isEmpty()) {
                log.warn("GitHub emails API returned empty list");
                return null;
            }

            for (Map<String, Object> emailEntry : emails) {
                Boolean primary = (Boolean) emailEntry.get("primary");
                Boolean verified = (Boolean) emailEntry.get("verified");
                if (Boolean.TRUE.equals(primary) && Boolean.TRUE.equals(verified)) {
                    return (String) emailEntry.get("email");
                }
            }

            for (Map<String, Object> emailEntry : emails) {
                Boolean verified = (Boolean) emailEntry.get("verified");
                if (Boolean.TRUE.equals(verified)) {
                    return (String) emailEntry.get("email");
                }
            }

            log.warn("No verified email found in GitHub emails API response");
            return null;
        } catch (RestClientException e) {
            log.error("Failed to fetch emails from GitHub: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
