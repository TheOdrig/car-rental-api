package com.akif.controller;

import com.akif.config.OAuth2Properties;
import com.akif.dto.response.AuthResponseDto;
import com.akif.shared.enums.AuthProvider;
import com.akif.shared.enums.Role;
import com.akif.model.User;
import com.akif.repository.LinkedAccountRepository;
import com.akif.repository.UserRepository;
import com.akif.service.oauth2.OAuth2StateService;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OAuth2Controller Integration Tests")
public class OAuth2ControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LinkedAccountRepository linkedAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private OAuth2StateService stateService;

    @Autowired
    private OAuth2Properties oAuth2Properties;

    @BeforeEach
    void setUp() {
        linkedAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        linkedAccountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Authorization Redirect Tests")
    class AuthorizationRedirectTests {

        @Test
        @DisplayName("Should redirect to Google authorization page")
        void shouldRedirectToGoogleAuthorizationPage() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/oauth2/authorize/google"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            String redirectUrl = result.getResponse().getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains(oAuth2Properties.getGoogle().getAuthorizationUri());
            assertThat(redirectUrl).contains("client_id=");
            assertThat(redirectUrl).contains("redirect_uri=");
            assertThat(redirectUrl).contains("scope=");
            assertThat(redirectUrl).contains("state=");
            assertThat(redirectUrl).contains("response_type=code");
        }

        @Test
        @DisplayName("Should redirect to GitHub authorization page")
        void shouldRedirectToGitHubAuthorizationPage() throws Exception {
            MvcResult result = mockMvc.perform(get("/api/oauth2/authorize/github"))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            String redirectUrl = result.getResponse().getRedirectedUrl();
            assertThat(redirectUrl).isNotNull();
            assertThat(redirectUrl).contains(oAuth2Properties.getGithub().getAuthorizationUri());
            assertThat(redirectUrl).contains("client_id=");
            assertThat(redirectUrl).contains("redirect_uri=");
            assertThat(redirectUrl).contains("scope=");
            assertThat(redirectUrl).contains("state=");
            assertThat(redirectUrl).contains("response_type=code");
        }
    }

    @Nested
    @DisplayName("Callback Tests")
    class CallbackTests {

        @Test
        @DisplayName("Should reject callback with invalid state")
        void shouldRejectCallbackWithInvalidState() throws Exception {
            String invalidState = "invalid-state-parameter";
            String authCode = "valid-auth-code";

            mockMvc.perform(get("/api/oauth2/callback/google")
                            .param("code", authCode)
                            .param("state", invalidState))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_state"));
        }

        @Test
        @DisplayName("Should reject callback with empty state")
        void shouldRejectCallbackWithEmptyState() throws Exception {
            mockMvc.perform(get("/api/oauth2/callback/google")
                            .param("code", "some-code")
                            .param("state", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_state"));
        }

        @Test
        @DisplayName("Should reject callback with expired state")
        void shouldRejectCallbackWithExpiredState() throws Exception {

            String expiredState = "expired-state-base64";

            mockMvc.perform(get("/api/oauth2/callback/github")
                            .param("code", "some-code")
                            .param("state", expiredState))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_state"));
        }
    }

    @Nested
    @DisplayName("Account Linking Tests")
    class AccountLinkingTests {

        @Test
        @DisplayName("Should reject linking when not authenticated")
        void shouldRejectLinkingWhenNotAuthenticated() throws Exception {
            String validState = stateService.generateState();
            String authCode = "valid-auth-code";

            mockMvc.perform(post("/api/oauth2/link/github")
                            .with(csrf())
                            .param("code", authCode)
                            .param("state", validState))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should reject linking with invalid state")
        void shouldRejectLinkingWithInvalidState() throws Exception {
            User existingUser = User.builder()
                    .username("stateuser")
                    .email("stateuser@example.com")
                    .password(passwordEncoder.encode("password123"))
                    .roles(Set.of(Role.USER))
                    .authProvider(AuthProvider.LOCAL)
                    .enabled(true)
                    .build();
            userRepository.save(existingUser);

            String accessToken = getAccessTokenForUser("stateuser", "password123");

            mockMvc.perform(post("/api/oauth2/link/github")
                            .with(csrf())
                            .header("Authorization", "Bearer " + accessToken)
                            .param("code", "valid-code")
                            .param("state", "invalid-state"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("invalid_state"));
        }
    }

    @Nested
    @DisplayName("State Parameter Security Tests")
    class StateParameterSecurityTests {

        @Test
        @DisplayName("Generated state should be valid")
        void generatedStateShouldBeValid() {
            String state = stateService.generateState();
            
            assertThat(state).isNotNull();
            assertThat(state).isNotBlank();

            assertThat(state).matches("^[A-Za-z0-9_-]+$");
        }

        @Test
        @DisplayName("Different states should be unique")
        void differentStatesShouldBeUnique() {
            String state1 = stateService.generateState();
            String state2 = stateService.generateState();
            
            assertThat(state1).isNotEqualTo(state2);
        }
    }

    private String getAccessTokenForUser(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType("application/json")
                        .content(String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponseDto authResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), AuthResponseDto.class);
        return authResponse.getAccessToken();
    }
}
