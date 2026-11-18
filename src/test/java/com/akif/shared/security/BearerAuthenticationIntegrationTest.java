package com.akif.shared.security;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Bearer token authentication")
class BearerAuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JwtEncoder jwtEncoder;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        user = userRepository.save(User.builder()
                .username("bearer-user")
                .email("bearer@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
    }

    private UsernamePasswordAuthenticationToken principal(String... roles) {
        return new UsernamePasswordAuthenticationToken("bearer-user", null,
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    @Test
    @DisplayName("an access token authenticates a protected endpoint")
    void accessTokenAuthenticates() throws Exception {
        String token = tokenService.generateAccessToken(principal("ROLE_USER"), user.getId());

        mockMvc.perform(get("/api/v1/users/me/profile").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bearer-user"));
    }

    @Test
    @DisplayName("a refresh token is rejected as an access token")
    void refreshTokenRejectedAsAccessToken() throws Exception {
        java.time.Instant now = java.time.Instant.now();
        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        JwtClaimsSet.builder()
                                .issuer("car-rental-api")
                                .subject("bearer-user")
                                .issuedAt(now)
                                .expiresAt(now.plusSeconds(600))
                                .claim(TokenUse.CLAIM, TokenUse.REFRESH.claimValue())
                                .build()))
                .getTokenValue();

        mockMvc.perform(get("/api/v1/users/me/profile").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    @DisplayName("roles in the token map to authorities")
    void rolesMapToAuthorities() throws Exception {
        String userToken = tokenService.generateAccessToken(principal("ROLE_USER"), user.getId());
        String adminToken = tokenService.generateAccessToken(principal("ROLE_USER", "ROLE_ADMIN"), user.getId());

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("every admin path requires the ADMIN role even without a controller rule")
    void adminPathsRequireAdminRole() throws Exception {
        String userToken = tokenService.generateAccessToken(principal("ROLE_USER"), user.getId());

        mockMvc.perform(get("/api/v1/admin/not-mapped-anywhere").header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a missing token yields a uniform 401 body")
    void missingTokenYields401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.instance").value("/api/v1/users/me/profile"));
    }

    @Test
    @DisplayName("a malformed token is rejected even on a public endpoint")
    void malformedTokenRejectedOnPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/cars").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a stale token is ignored on the login endpoint")
    void staleTokenIgnoredOnAuthEndpoints() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("Authorization", "Bearer not.a.jwt")
                        .contentType("application/json")
                        .content("{\"username\":\"bearer-user\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());
    }
}
