package com.akif.shared.security.ratelimit;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = {
        "security.rate-limit.enabled=true",
        "security.rate-limit.trust-forwarded-headers=false",
        "security.rate-limit.limits.login.capacity=3",
        "security.rate-limit.limits.login.period-seconds=60",
        "security.rate-limit.limits.register.capacity=2",
        "security.rate-limit.limits.register.period-seconds=600",
        "security.rate-limit.limits.refresh.capacity=2",
        "security.rate-limit.limits.refresh.period-seconds=60",
        "security.rate-limit.limits.password-reset.capacity=2",
        "security.rate-limit.limits.password-reset.period-seconds=600",
        "security.rate-limit.limits.oauth2-callback.capacity=2",
        "security.rate-limit.limits.oauth2-callback.period-seconds=60"
})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Rate limiting on authentication endpoints")
class RateLimitIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService.clear();
    }

    private MockHttpServletRequestBuilder json(String path, String body) {
        return post(path).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private void assertRateLimited(MvcResult result) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(429);
        assertThat(Long.parseLong(result.getResponse().getHeader("Retry-After"))).isPositive();
    }

    @Test
    @DisplayName("login is limited per client and answers 429 with Retry-After and the shared error body")
    void loginIsLimited() throws Exception {
        String body = "{\"username\":\"nobody\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json("/api/auth/login", body)).andExpect(status().isUnauthorized());
        }

        MvcResult limited = mockMvc.perform(json("/api/auth/login", body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.errorCode").value("RATE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.traceId").exists())
                .andReturn();

        assertRateLimited(limited);
    }

    @Test
    @DisplayName("register is limited")
    void registerIsLimited() throws Exception {
        String body = "{\"username\":\"x\",\"email\":\"bad\",\"password\":\"short\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(json("/api/auth/register", body)).andExpect(status().isBadRequest());
        }

        assertRateLimited(mockMvc.perform(json("/api/auth/register", body)).andReturn());
    }

    @Test
    @DisplayName("refresh is limited")
    void refreshIsLimited() throws Exception {
        String body = "{\"refreshToken\":\"unknown\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(json("/api/auth/refresh", body)).andExpect(status().isUnauthorized());
        }

        assertRateLimited(mockMvc.perform(json("/api/auth/refresh", body)).andReturn());
    }

    @Test
    @DisplayName("password reset requests are limited")
    void passwordResetIsLimited() throws Exception {
        String body = "{\"email\":\"someone@example.com\"}";
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(json("/api/auth/forgot-password", body)).andExpect(status().isOk());
        }

        assertRateLimited(mockMvc.perform(json("/api/auth/forgot-password", body)).andReturn());
    }

    @Test
    @DisplayName("the OAuth2 callback is limited")
    void oauth2CallbackIsLimited() throws Exception {
        for (int i = 0; i < 2; i++) {
            MvcResult result = mockMvc.perform(get("/api/oauth2/callback/google")
                    .param("code", "c").param("state", "invalid")).andReturn();
            assertThat(result.getResponse().getStatus()).isNotEqualTo(429);
        }

        assertRateLimited(mockMvc.perform(get("/api/oauth2/callback/google")
                .param("code", "c").param("state", "invalid")).andReturn());
    }

    @Test
    @DisplayName("forwarded headers do not create a separate bucket unless trusted")
    void forwardedHeadersIgnoredByDefault() throws Exception {
        String body = "{\"username\":\"nobody\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json("/api/auth/login", body).header("X-Forwarded-For", "198.51.100." + i))
                    .andExpect(status().isUnauthorized());
        }

        assertRateLimited(mockMvc.perform(json("/api/auth/login", body).header("X-Forwarded-For", "198.51.100.99"))
                .andReturn());
    }

    @Test
    @DisplayName("a different socket address has its own bucket")
    void differentClientsAreIndependent() throws Exception {
        String body = "{\"username\":\"nobody\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(json("/api/auth/login", body)).andExpect(status().isUnauthorized());
        }
        assertRateLimited(mockMvc.perform(json("/api/auth/login", body)).andReturn());

        mockMvc.perform(json("/api/auth/login", body).with(request -> {
                    request.setRemoteAddr("203.0.113.50");
                    return request;
                }))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("unthrottled endpoints are never limited")
    void otherEndpointsUnaffected() throws Exception {
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/cars")).andExpect(status().isOk());
        }
    }
}
