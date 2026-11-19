package com.akif.auth.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.AdminNoteRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin user management endpoints")
class AdminUserManagementIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminNoteRepository adminNoteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService tokenService;

    private User admin;
    private User customer;
    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        adminNoteRepository.deleteAll();
        userRepository.deleteAll();
        admin = userRepository.save(user("mgmtadmin", "mgmt-admin@example.com", Role.ADMIN));
        customer = userRepository.save(user("mgmtcustomer", "mgmt-customer@example.com", Role.USER));
        adminToken = "Bearer " + token(admin, "ROLE_ADMIN");
        customerToken = "Bearer " + token(customer, "ROLE_USER");
    }

    @AfterEach
    void tearDown() {
        adminNoteRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("counts total, active and banned users")
    void returnsStatistics() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users/stats").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.bannedUsers").value(0));
    }

    @Test
    @DisplayName("lists users and filters them by a search term")
    void listsAndSearchesUsers() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/v1/admin/users").param("search", "MGMT-CUSTOMER")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("mgmt-customer@example.com"));
    }

    @Test
    @DisplayName("bans and unbans a user, and refuses to repeat either step")
    void bansAndUnbansUser() throws Exception {
        String banUrl = "/api/v1/admin/users/" + customer.getId() + "/ban";
        String unbanUrl = "/api/v1/admin/users/" + customer.getId() + "/unban";

        mockMvc.perform(post(banUrl).header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Repeated policy violations\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User banned successfully"));
        mockMvc.perform(post(banUrl).header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Repeated policy violations\"}"))
                .andExpect(status().is4xxClientError());
        mockMvc.perform(get("/api/v1/admin/users/stats").header("Authorization", adminToken))
                .andExpect(jsonPath("$.bannedUsers").value(1));

        mockMvc.perform(post(unbanUrl).header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"Appeal accepted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User unbanned successfully"));
        mockMvc.perform(post(unbanUrl).header("Authorization", adminToken))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("rejects a ban reason that is too short")
    void rejectsShortBanReason() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/" + customer.getId() + "/ban")
                        .header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("adds admin notes and lists them")
    void addsAndListsNotes() throws Exception {
        String notesUrl = "/api/v1/admin/users/" + customer.getId() + "/notes";

        mockMvc.perform(post(notesUrl).header("Authorization", adminToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Called the customer about the late return\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.adminUsername").value("mgmtadmin"));

        mockMvc.perform(get(notesUrl).header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].text").value("Called the customer about the late return"));
    }

    @Test
    @DisplayName("answers 404 for notes of an unknown user")
    void unknownUserNotes() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users/999999/notes").header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"note\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("keeps every admin endpoint away from customers and anonymous callers")
    void protectsAdminEndpoints() throws Exception {
        for (String path : List.of("/api/v1/admin/users", "/api/v1/admin/users/stats",
                "/api/v1/admin/users/" + customer.getId() + "/notes")) {
            mockMvc.perform(get(path).header("Authorization", customerToken)).andExpect(status().isForbidden());
            mockMvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/v1/admin/users/" + customer.getId() + "/ban")
                        .header("Authorization", customerToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Repeated policy violations\"}"))
                .andExpect(status().isForbidden());
    }

    private User user(String username, String email, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode("password123"))
                .firstName("First")
                .lastName("Last")
                .roles(role == Role.USER ? Set.of(Role.USER) : Set.of(Role.USER, role))
                .enabled(true)
                .build();
    }

    private String token(User user, String role) {
        return tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, List.of(new SimpleGrantedAuthority(role))), user.getId());
    }
}
