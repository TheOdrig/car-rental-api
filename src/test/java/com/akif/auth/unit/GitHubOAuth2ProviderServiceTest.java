package com.akif.auth.unit;

import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.auth.internal.config.OAuth2Properties;
import com.akif.auth.internal.dto.oauth2.OAuth2TokenResponse;
import com.akif.auth.internal.dto.oauth2.OAuth2UserInfo;
import com.akif.auth.domain.exception.OAuth2AuthenticationException;
import com.akif.auth.internal.service.oauth2.impl.GitHubOAuth2ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("GitHubOAuth2ProviderService")
class GitHubOAuth2ProviderServiceTest {

    private static final String TOKEN_URI = "https://github.test/token";
    private static final String USER_URI = "https://github.test/user";
    private static final String EMAILS_URI = "https://github.test/user/emails";
    private static final String PROFILE = "{\"id\":42,\"login\":\"octo\",\"name\":\"Octo Cat\","
            + "\"email\":\"profile@example.com\",\"avatar_url\":\"https://avatars.test/42\"}";

    private MockRestServiceServer server;
    private GitHubOAuth2ProviderService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        OAuth2Properties properties = new OAuth2Properties();
        properties.getGithub().setTokenUri(TOKEN_URI);
        properties.getGithub().setUserInfoUri(USER_URI);
        properties.getGithub().setUserEmailsUri(EMAILS_URI);
        properties.getGithub().setClientId("client");
        properties.getGithub().setClientSecret("client-secret");
        properties.getGithub().setRedirectUri("https://app.test/callback");
        service = new GitHubOAuth2ProviderService(properties, builder.build());
    }

    @Test
    @DisplayName("uses the verified primary email from the emails endpoint, not the profile email")
    void usesVerifiedPrimaryEmail() {
        expectProfile();
        server.expect(requestTo(EMAILS_URI)).andExpect(header("Authorization", "Bearer token"))
                .andRespond(withSuccess("[{\"email\":\"other@example.com\",\"primary\":false,\"verified\":true},"
                        + "{\"email\":\"primary@example.com\",\"primary\":true,\"verified\":true}]",
                        MediaType.APPLICATION_JSON));

        OAuth2UserInfo info = service.getUserInfo("token");

        assertThat(info.email()).isEqualTo("primary@example.com");
        assertThat(info.providerId()).isEqualTo("42");
        assertThat(info.firstName()).isEqualTo("Octo");
        assertThat(info.lastName()).isEqualTo("Cat");
        assertThat(info.avatarUrl()).isEqualTo("https://avatars.test/42");
        server.verify();
    }

    @Test
    @DisplayName("refuses an unverified primary email and a verified non-primary one")
    void refusesUnverifiedPrimary() {
        expectProfile();
        server.expect(requestTo(EMAILS_URI)).andRespond(withSuccess(
                "[{\"email\":\"victim@example.com\",\"primary\":true,\"verified\":false},"
                        + "{\"email\":\"second@example.com\",\"primary\":false,\"verified\":true}]",
                MediaType.APPLICATION_JSON));

        assertErrorType(OAuth2ErrorType.EMAIL_REQUIRED);
    }

    @Test
    @DisplayName("fails with email required when the emails endpoint returns nothing")
    void failsWithoutEmails() {
        expectProfile();
        server.expect(requestTo(EMAILS_URI)).andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertErrorType(OAuth2ErrorType.EMAIL_REQUIRED);
    }

    @Test
    @DisplayName("reports the provider as unavailable when the user endpoint fails")
    void reportsUnavailableProvider() {
        server.expect(requestTo(USER_URI)).andRespond(withServerError());

        assertErrorType(OAuth2ErrorType.PROVIDER_UNAVAILABLE);
    }

    @Test
    @DisplayName("exchanges a code for an access token")
    void exchangesCode() {
        server.expect(requestTo(TOKEN_URI)).andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"access_token\":\"abc\",\"token_type\":\"bearer\"}",
                        MediaType.APPLICATION_JSON));

        OAuth2TokenResponse response = service.exchangeCodeForTokens("code");

        assertThat(response.accessToken()).isEqualTo("abc");
    }

    @Test
    @DisplayName("rejects a token response without an access token")
    void rejectsEmptyTokenResponse() {
        server.expect(requestTo(TOKEN_URI)).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.exchangeCodeForTokens("code"))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private void expectProfile() {
        server.expect(requestTo(USER_URI)).andRespond(withSuccess(PROFILE, MediaType.APPLICATION_JSON));
    }

    private void assertErrorType(OAuth2ErrorType expected) {
        assertThatThrownBy(() -> service.getUserInfo("token"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getErrorType())
                        .isEqualTo(expected));
    }
}
