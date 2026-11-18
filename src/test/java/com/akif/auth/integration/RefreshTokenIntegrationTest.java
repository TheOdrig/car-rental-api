package com.akif.auth.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.api.AdminUserService;
import com.akif.auth.domain.RefreshToken;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.RefreshTokenRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.auth.internal.service.RefreshTokenService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Refresh token lifecycle")
class RefreshTokenIntegrationTest {

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

    @Autowired
    private RefreshTokenService refreshTokenService;

    private User user;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .username("refresh-user")
                .email("refresh@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
    }

    private JsonNode login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"refresh-user\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private MvcResult refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andReturn();
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("stores only a SHA-256 hash of the opaque token")
    void storesOnlyHash() throws Exception {
        String raw = login().get("refreshToken").asText();

        List<RefreshToken> stored = refreshTokenRepository.findAll();

        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).getTokenHash()).isEqualTo(sha256(raw)).isNotEqualTo(raw);
        assertThat(raw).doesNotContain(".");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM car_rental.refresh_tokens WHERE token_hash = ?", Integer.class, raw)).isZero();
    }

    @Test
    @DisplayName("rotates the refresh token on every refresh")
    void rotatesOnEveryRefresh() throws Exception {
        String first = login().get("refreshToken").asText();

        MvcResult rotated = refresh(first);
        assertThat(rotated.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(rotated.getResponse().getContentAsString());
        String second = body.get("refreshToken").asText();

        assertThat(second).isNotEqualTo(first);
        assertThat(body.get("accessToken").asText()).isNotBlank();
        assertThat(body.get("tokenType").asText()).isEqualTo("Bearer");
        assertThat(body.get("expiresIn").asLong()).isEqualTo(900L);

        RefreshToken oldToken = refreshTokenRepository.findByTokenHash(sha256(first)).orElseThrow();
        RefreshToken newToken = refreshTokenRepository.findByTokenHash(sha256(second)).orElseThrow();
        assertThat(oldToken.isRevoked()).isTrue();
        assertThat(oldToken.getReplacedById()).isEqualTo(newToken.getId());
        assertThat(newToken.isRevoked()).isFalse();
        assertThat(newToken.getFamilyId()).isEqualTo(oldToken.getFamilyId());

        assertThat(refresh(second).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("a refreshed access token authenticates requests")
    void refreshedAccessTokenWorks() throws Exception {
        String first = login().get("refreshToken").asText();
        JsonNode body = objectMapper.readTree(refresh(first).getResponse().getContentAsString());

        mockMvc.perform(get("/api/users/me/profile")
                        .header("Authorization", "Bearer " + body.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    private void ageRotation(String rawToken) throws Exception {
        jdbcTemplate.update("UPDATE car_rental.refresh_tokens SET revoked_at = revoked_at - INTERVAL '2 days' "
                + "WHERE token_hash = ?", sha256(rawToken));
    }

    @Test
    @DisplayName("reuse of a rotated token inside the grace window is rejected without revoking the family")
    void reuseInsideGraceWindowKeepsFamily() throws Exception {
        String first = login().get("refreshToken").asText();
        String second = objectMapper.readTree(refresh(first).getResponse().getContentAsString())
                .get("refreshToken").asText();

        MvcResult reuse = refresh(first);

        assertThat(reuse.getResponse().getStatus()).isEqualTo(401);
        assertThat(refresh(second).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("reuse of a rotated token outside the grace window revokes the whole family")
    void reuseRevokesFamily() throws Exception {
        String first = login().get("refreshToken").asText();
        String second = objectMapper.readTree(refresh(first).getResponse().getContentAsString())
                .get("refreshToken").asText();
        ageRotation(first);

        MvcResult reuse = refresh(first);
        assertThat(reuse.getResponse().getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(reuse.getResponse().getContentAsString()).get("errorCode").asText())
                .isEqualTo("INVALID_TOKEN");

        assertThat(refresh(second).getResponse().getStatus()).isEqualTo(401);
        long active = refreshTokenRepository.findAll().stream().filter(token -> !token.isRevoked()).count();
        assertThat(active).isZero();
    }

    @Test
    @DisplayName("reuse in one session leaves other sessions untouched")
    void reuseDoesNotAffectOtherFamilies() throws Exception {
        String sessionA = login().get("refreshToken").asText();
        String sessionB = login().get("refreshToken").asText();

        refresh(sessionA);
        ageRotation(sessionA);
        assertThat(refresh(sessionA).getResponse().getStatus()).isEqualTo(401);

        assertThat(refresh(sessionB).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("logout revokes the refresh token")
    void logoutRevokesToken() throws Exception {
        String raw = login().get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + raw + "\"}"))
                .andExpect(status().isNoContent());

        assertThat(refresh(raw).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("logout with an unknown token is a no-op")
    void logoutWithUnknownTokenIsNoop() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"never-issued\"}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("an unknown refresh token is rejected")
    void unknownTokenRejected() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"never-issued\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"));
    }

    @Test
    @DisplayName("an expired refresh token is rejected")
    void expiredTokenRejected() throws Exception {
        String raw = login().get("refreshToken").asText();
        jdbcTemplate.update("UPDATE car_rental.refresh_tokens SET expires_at = issued_at - INTERVAL '1 minute'");

        MvcResult result = refresh(raw);

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).get("errorCode").asText())
                .isEqualTo("TOKEN_EXPIRED");
    }

    @Test
    @DisplayName("an opaque refresh token cannot authenticate a request")
    void refreshTokenRejectedAsBearer() throws Exception {
        String raw = login().get("refreshToken").asText();

        mockMvc.perform(get("/api/users/me/profile").header("Authorization", "Bearer " + raw))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("changing the password revokes every refresh token")
    void passwordChangeRevokesTokens() throws Exception {
        JsonNode session = login();

        mockMvc.perform(post("/api/users/me/password")
                        .header("Authorization", "Bearer " + session.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"password123\",\"newPassword\":\"another-password1\"}"))
                .andExpect(status().isOk());

        assertThat(refresh(session.get("refreshToken").asText()).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("banning a user revokes every refresh token")
    void banRevokesTokens() throws Exception {
        String raw = login().get("refreshToken").asText();

        adminUserService.banUser(user.getId(), "abuse", user.getId());

        assertThat(refresh(raw).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("two concurrent refreshes of one token yield exactly one success")
    void concurrentRefreshYieldsOneSuccess() throws Exception {
        String raw = login().get("refreshToken").asText();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<MvcResult> call = () -> refresh(raw);
            Future<MvcResult> a = executor.submit(call);
            Future<MvcResult> b = executor.submit(call);

            List<MvcResult> results = List.of(a.get(), b.get());
            List<Integer> statuses = results.stream().map(result -> result.getResponse().getStatus()).toList();

            assertThat(statuses).containsExactlyInAnyOrder(200, 401);
            MvcResult winner = results.stream().filter(result -> result.getResponse().getStatus() == 200)
                    .findFirst().orElseThrow();
            String winnerToken = objectMapper.readTree(winner.getResponse().getContentAsString())
                    .get("refreshToken").asText();
            assertThat(refresh(winnerToken).getResponse().getStatus()).isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("purges tokens that expired before the retention window")
    void purgesExpiredTokens() throws Exception {
        login();
        login();
        jdbcTemplate.update("UPDATE car_rental.refresh_tokens SET expires_at = issued_at - INTERVAL '8 days' "
                + "WHERE id = (SELECT MIN(id) FROM car_rental.refresh_tokens)");

        int removed = refreshTokenService.purgeExpired(Duration.ofDays(7));

        assertThat(removed).isEqualTo(1);
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }
}
