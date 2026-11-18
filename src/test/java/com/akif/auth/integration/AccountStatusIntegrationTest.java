package com.akif.auth.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.api.AdminUserService;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.RefreshTokenRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Account status gates sign-in and refresh")
class AccountStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AdminUserService adminUserService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .username("status-user")
                .email("status@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"status-user\",\"password\":\"password123\"}"))
                .andReturn();
    }

    private MvcResult refresh(String token) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + token + "\"}"))
                .andReturn();
    }

    @Test
    @DisplayName("a soft-deleted user cannot sign in with a password")
    void deletedUserCannotLogin() throws Exception {
        assertThat(login().getResponse().getStatus()).isEqualTo(200);
        jdbcTemplate.update("UPDATE car_rental.users SET is_deleted = true WHERE id = ?", user.getId());

        assertThat(login().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("a banned user cannot sign in with a password")
    void bannedUserCannotLogin() throws Exception {
        adminUserService.banUser(user.getId(), "abuse", user.getId());

        assertThat(login().getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("a soft-deleted user cannot refresh and loses every refresh token")
    void deletedUserCannotRefresh() throws Exception {
        String token = objectMapper.readTree(login().getResponse().getContentAsString())
                .get("refreshToken").asText();
        jdbcTemplate.update("UPDATE car_rental.users SET is_deleted = true WHERE id = ?", user.getId());

        assertThat(refresh(token).getResponse().getStatus()).isEqualTo(401);
        assertThat(refreshTokenRepository.findAll().stream().filter(t -> !t.isRevoked()).count()).isZero();
    }

    @Test
    @DisplayName("a user disabled directly in the database cannot refresh")
    void disabledUserCannotRefresh() throws Exception {
        String token = objectMapper.readTree(login().getResponse().getContentAsString())
                .get("refreshToken").asText();
        jdbcTemplate.update("UPDATE car_rental.users SET enabled = false WHERE id = ?", user.getId());

        assertThat(refresh(token).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("is_deleted is read through a single mapping")
    void softDeleteIsPersisted() {
        User loaded = userRepository.findById(user.getId()).orElseThrow();
        loaded.softDelete();
        userRepository.saveAndFlush(loaded);

        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_deleted FROM car_rental.users WHERE id = ?", Boolean.class, user.getId())).isTrue();
        assertThat(userRepository.findById(user.getId()).orElseThrow().isUsable()).isFalse();
    }
}
