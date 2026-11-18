package com.akif.shared.config;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("CORS in the security filter chain")
class CorsIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("a preflight request to a protected endpoint succeeds without authentication")
    void preflightSucceedsWithoutAuthentication() throws Exception {
        mockMvc.perform(options("/api/rentals/me")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("a preflight request from an unknown origin is rejected")
    void preflightFromUnknownOriginRejected() throws Exception {
        mockMvc.perform(options("/api/rentals/me")
                        .header("Origin", "https://evil.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("a rejected request still carries CORS headers for an allowed origin")
    void unauthenticatedResponseCarriesCorsHeaders() throws Exception {
        mockMvc.perform(get("/api/rentals/me").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    @DisplayName("the origin is never echoed back as a wildcard")
    void originIsNeverWildcard() throws Exception {
        mockMvc.perform(get("/api/cars").header("Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }
}
