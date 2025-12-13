package com.akif.auth.integration;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
import com.akif.auth.internal.dto.RegisterRequest;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        userRepository.deleteAll();

        User adminUser = User.builder()
                .username("admin")
                .email("admin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();

        userRepository.save(adminUser);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest("testuser", "test@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("Should login with valid credentials")
    void shouldLoginWithValidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("Should return 401 for invalid credentials")
    void shouldReturn401ForInvalidCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should access protected endpoint with valid JWT")
    void shouldAccessProtectedEndpointWithValidJWT() throws Exception {
        LoginRequest loginRequest = new LoginRequest("admin", "admin123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseContent, AuthResponse.class);

        mockMvc.perform(get("/api/cars")
                        .header("Authorization", "Bearer " + authResponse.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to car listing without JWT")
    void shouldAllowPublicAccessToCarListingWithoutJWT() throws Exception {
        mockMvc.perform(get("/api/cars"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow public access to car listing with invalid JWT")
    void shouldAllowPublicAccessToCarListingWithInvalidJWT() throws Exception {
        mockMvc.perform(get("/api/cars")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isOk());
    }
}

