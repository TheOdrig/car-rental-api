package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.internal.repository.RentalRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Soft-deleted cars")
class SoftDeletedCarVisibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private JwtTokenService tokenService;

    private Car live;
    private Car retired;

    @BeforeEach
    void setUp() {
        cleanUp();
        live = carRepository.save(TestDataBuilder.createAvailableCar());
        Car candidate = TestDataBuilder.createAvailableCar();
        candidate.setBrand("Retiredbrand");
        retired = carRepository.save(candidate);
        retired.softDelete();
        carRepository.save(retired);
    }

    @AfterEach
    void cleanUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String userBearer() {
        User user = userRepository.save(TestDataBuilder.createTestUser("renter"));
        return "Bearer " + tokenService.generateAccessToken(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()))),
                user.getId());
    }

    @Test
    @DisplayName("they are not listed")
    void notListed() throws Exception {
        mockMvc.perform(get("/api/v1/cars").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(live.getId()));
        mockMvc.perform(get("/api/v1/cars/active").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("they are not found by id or licence plate")
    void notShown() throws Exception {
        mockMvc.perform(get("/api/v1/cars/{id}", retired.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAR_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/cars/licensePlate/{plate}", retired.getLicensePlate())
                        .header("Authorization", userBearer()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAR_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/cars/{id}", live.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("they are not returned by search")
    void notSearchable() throws Exception {
        mockMvc.perform(get("/api/v1/cars/search").param("brand", "Retiredbrand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/cars/search/criteria").param("brand", "retired"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/cars/search/brand/{brand}", "Retiredbrand"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("they cannot be booked")
    void notBookable() throws Exception {
        String bearer = userBearer();
        RentalRequest request = new RentalRequest(retired.getId(), LocalDate.now().plusDays(10),
                LocalDate.now().plusDays(12), "trip");

        mockMvc.perform(post("/api/v1/rentals/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CAR_NOT_FOUND"));
    }
}
