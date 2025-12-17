package com.akif.dashboard.integration;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.dashboard.domain.enums.AlertSeverity;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenProvider;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AlertController Integration Tests")
class AlertControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private User testUser;
    private User adminUser;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        alertRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("alerttestuser")
                .email("alerttest@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        testUser = userRepository.save(testUser);

        adminUser = User.builder()
                .username("alertadmin")
                .email("alertadmin@example.com")
                .password(passwordEncoder.encode("admin123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();
        adminUser = userRepository.save(adminUser);

        Authentication userAuth = new UsernamePasswordAuthenticationToken(
                testUser.getUsername(), null,
                testUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );
        Authentication adminAuth = new UsernamePasswordAuthenticationToken(
                adminUser.getUsername(), null,
                adminUser.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                        .collect(Collectors.toList())
        );

        userToken = "Bearer " + tokenProvider.generateAccessToken(userAuth);
        adminToken = "Bearer " + tokenProvider.generateAccessToken(adminAuth);
    }

    @AfterEach
    void tearDown() {
        alertRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Alert createTestAlert(AlertType type, AlertSeverity severity, boolean acknowledged) {
        Alert alert = Alert.builder()
                .type(type)
                .severity(severity)
                .title("Test Alert - " + type.name())
                .message("This is a test alert message for " + type.name())
                .actionUrl("/api/admin/test/" + type.name().toLowerCase())
                .referenceId(1L)
                .acknowledged(acknowledged)
                .build();
        return alertRepository.save(alert);
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should return 403 when user tries to access alerts")
        void shouldReturn403WhenUserTriesToAccessAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 when no authentication provided")
        void shouldReturn403WhenNoAuthenticationProvided() throws Exception {
            mockMvc.perform(get("/api/admin/alerts"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 200 when admin accesses alerts")
        void shouldReturn200WhenAdminAccessesAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Get Active Alerts Tests")
    class GetActiveAlertsTests {

        @Test
        @DisplayName("Should return empty list when no alerts")
        void shouldReturnEmptyListWhenNoAlerts() throws Exception {
            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("Should return active alerts only")
        void shouldReturnActiveAlertsOnly() throws Exception {
            createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);
            createTestAlert(AlertType.FAILED_PAYMENT, AlertSeverity.HIGH, true);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].type").value("LATE_RETURN"))
                    .andExpect(jsonPath("$[0].acknowledged").value(false));
        }

        @Test
        @DisplayName("Should return alerts sorted by severity")
        void shouldReturnAlertsSortedBySeverity() throws Exception {
            createTestAlert(AlertType.LOW_AVAILABILITY, AlertSeverity.WARNING, false);
            createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);
            createTestAlert(AlertType.FAILED_PAYMENT, AlertSeverity.HIGH, false);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(3))
                    .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                    .andExpect(jsonPath("$[1].severity").value("HIGH"))
                    .andExpect(jsonPath("$[2].severity").value("WARNING"));
        }

        @Test
        @DisplayName("Should filter alerts by type")
        void shouldFilterAlertsByType() throws Exception {
            createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);
            createTestAlert(AlertType.FAILED_PAYMENT, AlertSeverity.HIGH, false);
            createTestAlert(AlertType.LOW_AVAILABILITY, AlertSeverity.WARNING, false);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken)
                            .param("type", "LATE_RETURN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].type").value("LATE_RETURN"));
        }
    }

    @Nested
    @DisplayName("Acknowledge Alert Tests")
    class AcknowledgeAlertTests {

        @Test
        @DisplayName("Should acknowledge alert successfully")
        void shouldAcknowledgeAlertSuccessfully() throws Exception {
            Alert alert = createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);

            mockMvc.perform(post("/api/admin/alerts/{id}/acknowledge", alert.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(alert.getId()))
                    .andExpect(jsonPath("$.acknowledged").value(true))
                    .andExpect(jsonPath("$.acknowledgedBy").value("alertadmin"))
                    .andExpect(jsonPath("$.acknowledgedAt").exists());

            Alert updatedAlert = alertRepository.findById(alert.getId()).orElseThrow();
            assertThat(updatedAlert.isAcknowledged()).isTrue();
            assertThat(updatedAlert.getAcknowledgedBy()).isEqualTo("alertadmin");
        }

        @Test
        @DisplayName("Should return 404 when alert not found")
        void shouldReturn404WhenAlertNotFound() throws Exception {
            mockMvc.perform(post("/api/admin/alerts/{id}/acknowledge", 99999L)
                            .header("Authorization", adminToken))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 403 when user tries to acknowledge alert")
        void shouldReturn403WhenUserTriesToAcknowledgeAlert() throws Exception {
            Alert alert = createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);

            mockMvc.perform(post("/api/admin/alerts/{id}/acknowledge", alert.getId())
                            .header("Authorization", userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should not show acknowledged alert in active alerts")
        void shouldNotShowAcknowledgedAlertInActiveAlerts() throws Exception {
            Alert alert = createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);

            mockMvc.perform(post("/api/admin/alerts/{id}/acknowledge", alert.getId())
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Alert Data Validation Tests")
    class AlertDataValidationTests {

        @Test
        @DisplayName("Should return alert with all expected fields")
        void shouldReturnAlertWithAllExpectedFields() throws Exception {
            Alert alert = createTestAlert(AlertType.LATE_RETURN, AlertSeverity.CRITICAL, false);

            mockMvc.perform(get("/api/admin/alerts")
                            .header("Authorization", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(alert.getId()))
                    .andExpect(jsonPath("$[0].type").value("LATE_RETURN"))
                    .andExpect(jsonPath("$[0].severity").value("CRITICAL"))
                    .andExpect(jsonPath("$[0].title").value("Test Alert - LATE_RETURN"))
                    .andExpect(jsonPath("$[0].message").exists())
                    .andExpect(jsonPath("$[0].actionUrl").exists())
                    .andExpect(jsonPath("$[0].acknowledged").value(false))
                    .andExpect(jsonPath("$[0].createdAt").exists());
        }
    }
}
