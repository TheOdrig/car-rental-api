package com.akif.auth.integration;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
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

import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ProfileController Integration Tests")
class ProfileControllerIntegrationTest {

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
                .phone("+1234567890")
                .roles(new HashSet<>(Set.of(Role.USER)))
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
    @DisplayName("GET /api/users/me/profile")
    class GetProfile {

        @Test
        @DisplayName("Should return profile with valid JWT")
        void shouldReturnProfileWithValidJwt() throws Exception {
            mockMvc.perform(get("/api/users/me/profile")
                    .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.firstName").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("User"))
                    .andExpect(jsonPath("$.phone").value("+1234567890"));
        }

        @Test
        @DisplayName("Should return 401 without JWT")
        void shouldReturn401WithoutJwt() throws Exception {
            mockMvc.perform(get("/api/users/me/profile"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 401 with invalid JWT")
        void shouldReturn401WithInvalidJwt() throws Exception {
            mockMvc.perform(get("/api/users/me/profile")
                    .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PUT /api/users/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfileSuccessfully() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "+9876543210");

            mockMvc.perform(put("/api/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.phone").value("+9876543210"));
        }

        @Test
        @DisplayName("Should clear phone when empty string provided")
        void shouldClearPhoneWhenEmptyStringProvided() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "");

            mockMvc.perform(put("/api/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.phone").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 with invalid data")
        void shouldReturn400WithInvalidData() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("", "", null);

            mockMvc.perform(put("/api/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 without JWT")
        void shouldReturn401WithoutJwt() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", null);

            mockMvc.perform(put("/api/users/me/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
