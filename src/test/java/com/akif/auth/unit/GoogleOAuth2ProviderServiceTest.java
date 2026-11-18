package com.akif.auth.unit;

import com.akif.auth.domain.enums.OAuth2ErrorType;
import com.akif.auth.internal.config.OAuth2Properties;
import com.akif.auth.internal.dto.oauth2.OAuth2UserInfo;
import com.akif.auth.internal.exception.OAuth2AuthenticationException;
import com.akif.auth.internal.service.oauth2.impl.GoogleOAuth2ProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@DisplayName("GoogleOAuth2ProviderService user info")
class GoogleOAuth2ProviderServiceTest {

    private static final String USER_INFO_URI = "https://google.test/userinfo";

    private MockRestServiceServer server;
    private GoogleOAuth2ProviderService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        OAuth2Properties properties = new OAuth2Properties();
        properties.getGoogle().setUserInfoUri(USER_INFO_URI);
        service = new GoogleOAuth2ProviderService(properties, builder.build());
    }

    @Test
    @DisplayName("accepts a verified email")
    void acceptsVerifiedEmail() {
        server.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(
                "{\"sub\":\"1\",\"email\":\"alice@example.com\",\"email_verified\":true,\"name\":\"Alice\"}",
                MediaType.APPLICATION_JSON));

        OAuth2UserInfo info = service.getUserInfo("token");

        assertThat(info.email()).isEqualTo("alice@example.com");
        assertThat(info.providerId()).isEqualTo("1");
    }

    @Test
    @DisplayName("refuses an unverified email so it cannot be linked to a local account")
    void refusesUnverifiedEmail() {
        server.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(
                "{\"sub\":\"1\",\"email\":\"victim@example.com\",\"email_verified\":false}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.getUserInfo("token"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(error -> assertThat(((OAuth2AuthenticationException) error).getErrorType())
                        .isEqualTo(OAuth2ErrorType.EMAIL_REQUIRED));
    }

    @Test
    @DisplayName("refuses a response without the verification flag")
    void refusesMissingVerificationFlag() {
        server.expect(requestTo(USER_INFO_URI)).andRespond(withSuccess(
                "{\"sub\":\"1\",\"email\":\"victim@example.com\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.getUserInfo("token"))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }
}
