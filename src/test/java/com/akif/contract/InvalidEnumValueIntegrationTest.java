package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.shared.enums.Role;
import com.akif.shared.security.JwtTokenService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Unknown enum values answer INVALID_PARAMETER")
class InvalidEnumValueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenService tokenService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User admin = userRepository.save(User.builder()
                .username("enumadmin")
                .email("enum.admin@example.com")
                .password("x")
                .firstName("Enum")
                .lastName("Admin")
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build());
        adminToken = "Bearer " + tokenService.generateAccessToken(new UsernamePasswordAuthenticationToken(
                admin.getUsername(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))), admin.getId());
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    private ResultActions expectInvalid(ResultActions result, String field) throws Exception {
        return result
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.errors[0].field").value(field))
                .andExpect(content().string(not(containsString("com.akif"))))
                .andExpect(content().string(not(containsString("java.lang"))))
                .andExpect(content().string(not(containsString("Failed to convert"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XXX", "usd", "Usd"})
    @DisplayName("a currency query parameter must be an exact code")
    void currencyQuery(String value) throws Exception {
        expectInvalid(mockMvc.perform(get("/api/v1/cars").param("currency", value)), "currency");
    }

    @Test
    @DisplayName("a car search filter bound from the query string")
    void carSearchModelAttribute() throws Exception {
        expectInvalid(mockMvc.perform(get("/api/v1/cars/search").param("carStatusType", "FOO")), "carStatusType");
    }

    @Test
    @DisplayName("admin filters for damages, cars, alerts and late returns")
    void adminFilters() throws Exception {
        expectInvalid(mockMvc.perform(get("/api/v1/admin/damages/search").param("severity", "SEVERE")
                .header("Authorization", adminToken)), "severity");
        expectInvalid(mockMvc.perform(get("/api/v1/admin/cars").param("status", "FOO")
                .header("Authorization", adminToken)), "status");
        expectInvalid(mockMvc.perform(get("/api/v1/admin/alerts").param("type", "FOO")
                .header("Authorization", adminToken)), "type");
        expectInvalid(mockMvc.perform(get("/api/v1/admin/late-returns").param("status", "FOO")
                .header("Authorization", adminToken)), "status");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FOO", "confirmed", "Confirmed", "In Use"})
    @DisplayName("the admin rental list accepts status codes only")
    void adminRentalStatus(String value) throws Exception {
        expectInvalid(mockMvc.perform(get("/api/v1/rentals/admin").param("status", value)
                .header("Authorization", adminToken)), "status");
        expectInvalid(mockMvc.perform(get("/api/v1/rentals/admin").param("status", "CONFIRMED," + value)
                .header("Authorization", adminToken)), "status");
    }

    @Test
    @DisplayName("an enum in a request body")
    void enumInBody() throws Exception {
        expectInvalid(mockMvc.perform(post("/api/v1/cars")
                .header("Authorization", adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"licensePlate\":\"34ABC123\",\"brand\":\"Toyota\",\"model\":\"Corolla\","
                        + "\"productionYear\":2020,\"price\":100,\"currencyType\":\"usd\",\"carStatusType\":\"AVAILABLE\"}")),
                "currencyType");
    }

    @Test
    @DisplayName("a value of the wrong kind that is not an enum keeps INVALID_PARAMETER_TYPE")
    void nonEnumTypeMismatch() throws Exception {
        mockMvc.perform(get("/api/v1/cars/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER_TYPE"));
    }
}
