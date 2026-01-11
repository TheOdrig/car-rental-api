package com.akif.shared.security;

import com.akif.car.domain.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.car.domain.Car;
import com.akif.auth.domain.User;
import com.akif.car.internal.repository.CarRepository;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("SecurityConfig Integration Tests")
public class SecurityConfigTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private MockMvc mockMvc;
    private User testUser;
    private User adminUser;
    private Car testCar;
    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        adminUser = User.builder()
                .username("adminuser")
                .email("admin@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER, Role.ADMIN))
                .enabled(true)
                .build();

        userRepository.saveAll(Set.of(testUser, adminUser));

        testCar = Car.builder()
                .brand("Tesla")
                .model("Model 3")
                .price(BigDecimal.valueOf(45000))
                .currencyType(CurrencyType.USD)
                .productionYear(2023)
                .carStatusType(CarStatusType.AVAILABLE)
                .licensePlate("34TEST1234")
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .damagePrice(BigDecimal.ZERO)
                .build();

        testCar = carRepository.save(testCar);

        userToken = jwtTokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(
                        testUser.getUsername(), null, testUser.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList()
                ), testUser.getId()
        );

        adminToken = jwtTokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(
                        adminUser.getUsername(), null, adminUser.getRoles().stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .toList()
                ), adminUser.getId()
        );
    }

    @Nested
    @DisplayName("Public Endpoint Access Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should allow access to public endpoints without JWT token")
        void shouldAllowAccessToPublicEndpointsWithoutJWT() throws Exception {
            mockMvc.perform(get("/api/cars"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access to car details without JWT token")
        void shouldAllowAccessToCarDetailsWithoutJWT() throws Exception {
            mockMvc.perform(get("/api/cars/" + testCar.getId()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access to Swagger UI without JWT token")
        void shouldAllowAccessToSwaggerUI() throws Exception {
            mockMvc.perform(get("/swagger-ui/index.html"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow access to API docs without JWT token")
        void shouldAllowAccessToApiDocs() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("JWT Token Validation Tests")
    class JwtTokenValidationTests {

        @Test
        @DisplayName("Should deny access to protected endpoint without JWT token")
        void shouldDenyAccessWithoutToken() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access with invalid JWT token")
        void shouldDenyAccessWithInvalidToken() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer invalid.token.here"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access with malformed Authorization header")
        void shouldDenyAccessWithMalformedHeader() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "InvalidFormat token"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access with empty Bearer token")
        void shouldDenyAccessWithEmptyBearerToken() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer "))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should deny access with empty Authorization header")
        void shouldDenyAccessWithEmptyAuthHeader() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", ""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow access to protected endpoint with valid JWT token")
        void shouldAllowAccessWithValidToken() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("Role-Based Access Control Tests")
    class RoleBasedAccessTests {

        @Test
        @DisplayName("Should allow USER role to access public endpoints with JWT")
        void shouldAllowUserRoleToAccessPublicEndpoints() throws Exception {
            mockMvc.perform(get("/api/cars")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should deny USER role access to ADMIN-only business endpoints")
        void shouldDenyUserRoleAccessToAdminEndpoints() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow ADMIN role to access business endpoints")
        void shouldAllowAdminRoleToAccessBusinessEndpoints() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should allow ADMIN role to access car listing with JWT")
        void shouldAllowAdminRoleToAccessCarListing() throws Exception {
            mockMvc.perform(get("/api/cars")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should deny USER role to delete cars")
        void shouldDenyUserRoleToDeleteCars() throws Exception {
            mockMvc.perform(delete("/api/cars/" + testCar.getId())
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should allow ADMIN role to delete cars")
        void shouldAllowAdminRoleToDeleteCars() throws Exception {
            mockMvc.perform(delete("/api/cars/" + testCar.getId())
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("Authentication vs Authorization Tests")
    class AuthenticationVsAuthorizationTests {

        @Test
        @DisplayName("Should return 403 for missing JWT on protected endpoint")
        void shouldReturn403ForMissingJWT() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 403 for insufficient role with valid JWT")
        void shouldReturn403ForInsufficientRole() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Should return 200 for sufficient role with valid JWT")
        void shouldReturn200ForSufficientRole() throws Exception {
            mockMvc.perform(get("/api/cars/business/" + testCar.getId() + "/can-sell")
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());
        }
    }
}
