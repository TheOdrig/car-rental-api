package com.akif.auth.integration;

import com.akif.auth.api.AuthResponse;
import com.akif.auth.domain.User;
import com.akif.auth.internal.dto.LoginRequest;
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
import org.springframework.mock.web.MockMultipartFile;
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
@DisplayName("AvatarController Integration Tests")
class AvatarControllerIntegrationTest {

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
        @DisplayName("POST /api/users/me/avatar")
        class UploadAvatar {

                @Test
                @DisplayName("Should upload avatar successfully with valid image")
                void shouldUploadAvatarSuccessfullyWithValidImage() throws Exception {
                        MockMultipartFile avatarFile = new MockMultipartFile(
                                        "avatar",
                                        "avatar.jpg",
                                        "image/jpeg",
                                        "test image content".getBytes());

                        mockMvc.perform(multipart("/api/users/me/avatar")
                                        .file(avatarFile)
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.avatarUrl").exists())
                                        .andExpect(jsonPath("$.message").value("Avatar uploaded successfully"));
                }

                @Test
                @DisplayName("Should return 400 for invalid file type")
                void shouldReturn400ForInvalidFileType() throws Exception {
                        MockMultipartFile pdfFile = new MockMultipartFile(
                                        "avatar",
                                        "document.pdf",
                                        "application/pdf",
                                        "pdf content".getBytes());

                        mockMvc.perform(multipart("/api/users/me/avatar")
                                        .file(pdfFile)
                                        .header("Authorization", userToken))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @DisplayName("Should return 401 without JWT")
                void shouldReturn401WithoutJwt() throws Exception {
                        MockMultipartFile avatarFile = new MockMultipartFile(
                                        "avatar",
                                        "avatar.jpg",
                                        "image/jpeg",
                                        "test image content".getBytes());

                        mockMvc.perform(multipart("/api/users/me/avatar")
                                        .file(avatarFile))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should accept PNG file")
                void shouldAcceptPngFile() throws Exception {
                        MockMultipartFile pngFile = new MockMultipartFile(
                                        "avatar",
                                        "avatar.png",
                                        "image/png",
                                        "png content".getBytes());

                        mockMvc.perform(multipart("/api/users/me/avatar")
                                        .file(pngFile)
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.avatarUrl").exists());
                }
        }

        @Nested
        @DisplayName("DELETE /api/users/me/avatar")
        class DeleteAvatar {

                @Test
                @DisplayName("Should delete avatar successfully")
                void shouldDeleteAvatarSuccessfully() throws Exception {
                        mockMvc.perform(delete("/api/users/me/avatar")
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Avatar deleted successfully"));
                }

                @Test
                @DisplayName("Should return 401 without JWT")
                void shouldReturn401WithoutJwt() throws Exception {
                        mockMvc.perform(delete("/api/users/me/avatar"))
                                        .andExpect(status().isForbidden());
                }

                @Test
                @DisplayName("Should handle delete when no avatar exists")
                void shouldHandleDeleteWhenNoAvatarExists() throws Exception {
                        mockMvc.perform(delete("/api/users/me/avatar")
                                        .header("Authorization", userToken))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.message").value("Avatar deleted successfully"));
                }
        }
}
