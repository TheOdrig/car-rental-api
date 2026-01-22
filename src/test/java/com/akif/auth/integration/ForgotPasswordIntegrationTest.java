package com.akif.auth.integration;

import com.akif.auth.domain.PasswordResetToken;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.ForgotPasswordRequest;
import com.akif.auth.internal.dto.ResetPasswordRequest;
import com.akif.auth.internal.repository.PasswordResetTokenRepository;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("ForgotPassword Integration Tests")
class ForgotPasswordIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordResetTokenRepository tokenRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        private MockMvc mockMvc;
        private User testUser;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders
                                .webAppContextSetup(webApplicationContext)
                                .apply(springSecurity())
                                .build();

                tokenRepository.deleteAll();
                userRepository.deleteAll();

                testUser = User.builder()
                                .username("testuser")
                                .email("test@example.com")
                                .password(passwordEncoder.encode("password123"))
                                .firstName("Test")
                                .lastName("User")
                                .roles(new HashSet<>(Set.of(Role.USER)))
                                .enabled(true)
                                .build();
                userRepository.save(testUser);
        }

        @Nested
        @DisplayName("POST /api/auth/forgot-password")
        class ForgotPassword {

                @Test
                @DisplayName("Should accept request for existing email")
                void shouldAcceptRequestForExistingEmail() throws Exception {
                        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

                        mockMvc.perform(post("/api/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());

                        assertThat(tokenRepository.findAll()).hasSize(1);
                }

                @Test
                @DisplayName("Should accept request for non-existing email without error")
                void shouldAcceptRequestForNonExistingEmailWithoutError() throws Exception {
                        ForgotPasswordRequest request = new ForgotPasswordRequest("nonexistent@example.com");

                        mockMvc.perform(post("/api/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());

                        assertThat(tokenRepository.findAll()).isEmpty();
                }

                @Test
                @DisplayName("Should return 400 for invalid email format")
                void shouldReturn400ForInvalidEmailFormat() throws Exception {
                        ForgotPasswordRequest request = new ForgotPasswordRequest("invalid-email");

                        mockMvc.perform(post("/api/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should delete old tokens when requesting new one")
                void shouldDeleteOldTokensWhenRequestingNewOne() throws Exception {
                        PasswordResetToken oldToken = PasswordResetToken.builder()
                                        .token(UUID.randomUUID().toString())
                                        .email("test@example.com")
                                        .expiryDate(LocalDateTime.now().plusHours(1))
                                        .used(false)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        tokenRepository.save(oldToken);

                        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");

                        mockMvc.perform(post("/api/auth/forgot-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());

                        assertThat(tokenRepository.findAll()).hasSize(1);
                        assertThat(tokenRepository.findAll().get(0).getToken()).isNotEqualTo(oldToken.getToken());
                }
        }

        @Nested
        @DisplayName("POST /api/auth/reset-password")
        class ResetPassword {

                @Test
                @DisplayName("Should reset password with valid token")
                void shouldResetPasswordWithValidToken() throws Exception {
                        String tokenValue = UUID.randomUUID().toString();
                        PasswordResetToken token = PasswordResetToken.builder()
                                        .token(tokenValue)
                                        .email("test@example.com")
                                        .expiryDate(LocalDateTime.now().plusHours(1))
                                        .used(false)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        tokenRepository.save(token);

                        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "newPassword456");

                        mockMvc.perform(post("/api/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isOk());

                        User updatedUser = userRepository.findByEmail("test@example.com").orElseThrow();
                        assertThat(passwordEncoder.matches("newPassword456", updatedUser.getPassword())).isTrue();

                        PasswordResetToken usedToken = tokenRepository.findAll().get(0);
                        assertThat(usedToken.getUsed()).isTrue();
                }

                @Test
                @DisplayName("Should return 400 for expired token")
                void shouldReturn400ForExpiredToken() throws Exception {
                        String tokenValue = UUID.randomUUID().toString();
                        PasswordResetToken token = PasswordResetToken.builder()
                                        .token(tokenValue)
                                        .email("test@example.com")
                                        .expiryDate(LocalDateTime.now().minusHours(1))
                                        .used(false)
                                        .createdAt(LocalDateTime.now().minusHours(2))
                                        .build();
                        tokenRepository.save(token);

                        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "newPassword456");

                        mockMvc.perform(post("/api/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 for invalid token")
                void shouldReturn400ForInvalidToken() throws Exception {
                        ResetPasswordRequest request = new ResetPasswordRequest("invalid-token", "newPassword456");

                        mockMvc.perform(post("/api/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 for already used token")
                void shouldReturn400ForAlreadyUsedToken() throws Exception {
                        String tokenValue = UUID.randomUUID().toString();
                        PasswordResetToken token = PasswordResetToken.builder()
                                        .token(tokenValue)
                                        .email("test@example.com")
                                        .expiryDate(LocalDateTime.now().plusHours(1))
                                        .used(true)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        tokenRepository.save(token);

                        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "newPassword456");

                        mockMvc.perform(post("/api/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 400 for short password")
                void shouldReturn400ForShortPassword() throws Exception {
                        String tokenValue = UUID.randomUUID().toString();
                        PasswordResetToken token = PasswordResetToken.builder()
                                        .token(tokenValue)
                                        .email("test@example.com")
                                        .expiryDate(LocalDateTime.now().plusHours(1))
                                        .used(false)
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        tokenRepository.save(token);

                        ResetPasswordRequest request = new ResetPasswordRequest(tokenValue, "short");

                        mockMvc.perform(post("/api/auth/reset-password")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)))
                                        .andExpect(status().isBadRequest());
                }
        }
}
