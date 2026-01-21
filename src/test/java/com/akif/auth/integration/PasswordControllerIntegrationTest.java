package com.akif.auth.integration;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.PasswordChangeRequest;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordController Integration Tests")
class PasswordControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String userToken;
    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .firstName("Test")
                .lastName("User")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        userRepository.save(testUser);

        userToken = getTokenForUser("testuser", "password123");
    }

    private String getTokenForUser(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return "Bearer " + authResponse.accessToken();
    }

    @Nested
    @DisplayName("POST /api/users/me/password")
    class ChangePassword {

        @Test
        @DisplayName("Should change password successfully")
        void shouldChangePasswordSuccessfully() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("password123", "newPassword456");

            mockMvc.perform(post("/api/users/me/password")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 when current password is incorrect")
        void shouldReturn400WhenCurrentPasswordIsIncorrect() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("wrongPassword", "newPassword456");

            mockMvc.perform(post("/api/users/me/password")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when new password is same as current")
        void shouldReturn400WhenNewPasswordIsSameAsCurrent() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("password123", "password123");

            mockMvc.perform(post("/api/users/me/password")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when new password is too short")
        void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("password123", "short");

            mockMvc.perform(post("/api/users/me/password")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 without JWT")
        void shouldReturn401WithoutJwt() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("password123", "newPassword456");

            mockMvc.perform(post("/api/users/me/password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should allow login with new password after change")
        void shouldAllowLoginWithNewPasswordAfterChange() throws Exception {
            PasswordChangeRequest request = new PasswordChangeRequest("password123", "newPassword456");

            mockMvc.perform(post("/api/users/me/password")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            LoginRequest loginRequest = new LoginRequest("testuser", "newPassword456");
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }
    }
}
