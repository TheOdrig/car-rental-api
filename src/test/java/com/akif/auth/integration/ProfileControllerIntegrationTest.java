package com.akif.auth.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.ProfileUpdateRequest;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.akif.testsupport.TestcontainersConfiguration;
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

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
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

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        return "Bearer " + authResponse.accessToken();
    }

    @Nested
    @DisplayName("GET /api/v1/users/me/profile")
    class GetProfile {

        @Test
        @DisplayName("Should return profile with valid JWT")
        void shouldReturnProfileWithValidJwt() throws Exception {
            mockMvc.perform(get("/api/v1/users/me/profile")
                    .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.firstName").value("Test"))
                    .andExpect(jsonPath("$.lastName").value("User"))
                    .andExpect(jsonPath("$.phone").value("+1234567890"));
        }

        @Test
        @DisplayName("Should report emailVerified false for a fresh account and true once verified")
        void shouldExposeEmailVerified() throws Exception {
            mockMvc.perform(get("/api/v1/users/me/profile")
                    .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(false));

            User stored = userRepository.findByUsername("testuser").orElseThrow();
            org.springframework.test.util.ReflectionTestUtils.setField(stored, "emailVerified", true);
            userRepository.saveAndFlush(stored);

            mockMvc.perform(get("/api/v1/users/me/profile")
                    .header("Authorization", userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(true));
            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(new ProfileUpdateRequest("John", "Doe", "+9876543210"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.emailVerified").value(true));
        }

        @Test
        @DisplayName("Should return 401 without JWT")
        void shouldReturn401WithoutJwt() throws Exception {
            mockMvc.perform(get("/api/v1/users/me/profile"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 401 with invalid JWT")
        void shouldReturn401WithInvalidJwt() throws Exception {
            mockMvc.perform(get("/api/v1/users/me/profile")
                    .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/users/me/profile")
    class UpdateProfile {

        @Test
        @DisplayName("Should update profile successfully")
        void shouldUpdateProfileSuccessfully() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "+9876543210");

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.phone").value("+9876543210"));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"12345", "+123456", "abc1234567", "+1234567890123456", "0123456789", "123 456 7890"})
        @DisplayName("Should reject an implausible phone number with a field error")
        void shouldRejectInvalidPhone(String phone) throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", phone);

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.errors[0].field").value("phone"));
        }

        @org.junit.jupiter.params.ParameterizedTest
        @org.junit.jupiter.params.provider.ValueSource(strings = {"+1234567", "905551234567", "+441234567890123"})
        @DisplayName("Should accept phone numbers of 7 to 15 digits")
        void shouldAcceptPlausiblePhone(String phone) throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", phone);

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.phone").value(phone));
        }

        @Test
        @DisplayName("Should clear phone when empty string provided")
        void shouldClearPhoneWhenEmptyStringProvided() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", "");

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.phone").isEmpty());
        }

        @Test
        @DisplayName("Should save a phone for a user without names and store blank names as null")
        void shouldAllowBlankNames() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("  ", "", "+905551112233");

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").doesNotExist())
                    .andExpect(jsonPath("$.lastName").doesNotExist())
                    .andExpect(jsonPath("$.phone").value("+905551112233"));
        }

        @Test
        @DisplayName("Should return 400 when a name is too long")
        void shouldReturn400WithInvalidData() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("x".repeat(51), "Doe", null);

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .header("Authorization", userToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 401 without JWT")
        void shouldReturn401WithoutJwt() throws Exception {
            ProfileUpdateRequest request = new ProfileUpdateRequest("John", "Doe", null);

            mockMvc.perform(put("/api/v1/users/me/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
