package com.akif.car.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.dashboard.api.DashboardService;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.enums.LateReturnStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import com.akif.testsupport.TestcontainersConfiguration;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class, properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Admin fleet occupancy")
class AdminFleetOccupancyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private Clock clock;

    private Car inUse;
    private Car reserved;
    private Car upcoming;
    private Car free;
    private Rental inUseRental;
    private Rental reservedRental;
    private User customer;

    @BeforeEach
    void setUp() {
        tearDown();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        customer = userRepository.save(User.builder()
                .username("occupancycustomer")
                .email("occupancy.customer@example.com")
                .password("x")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build());
        LocalDate today = LocalDate.now(clock);
        inUse = car("34OCC001", CarStatusType.AVAILABLE);
        reserved = car("34OCC002", CarStatusType.AVAILABLE);
        upcoming = car("34OCC003", CarStatusType.AVAILABLE);
        free = car("34OCC004", CarStatusType.AVAILABLE);
        Car inUseInMaintenance = car("34OCC005", CarStatusType.MAINTENANCE);
        inUseRental = rental(inUse, RentalStatus.IN_USE, today.minusDays(1), today.plusDays(2));
        reservedRental = rental(reserved, RentalStatus.CONFIRMED, today, today.plusDays(3));
        rental(upcoming, RentalStatus.CONFIRMED, today.plusDays(10), today.plusDays(12));
        rental(free, RentalStatus.RETURNED, today.minusDays(9), today.minusDays(7));
        rental(free, RentalStatus.REQUESTED, today, today.plusDays(1));
        rental(free, RentalStatus.CANCELLED, today.plusDays(4), today.plusDays(5));
        rental(inUseInMaintenance, RentalStatus.IN_USE, today.minusDays(1), today.plusDays(1));
    }

    @AfterEach
    void tearDown() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private Car car(String plate, CarStatusType status) {
        return carRepository.save(Car.builder()
                .licensePlate(plate)
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2021)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(status)
                .build());
    }

    private Rental rental(Car car, RentalStatus status, LocalDate start, LocalDate end) {
        return rentalRepository.save(Rental.builder()
                .userId(customer.getId())
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .userEmail(customer.getEmail())
                .userFullName("Occupancy Customer")
                .startDate(start)
                .endDate(end)
                .days((int) ChronoUnit.DAYS.between(start, end) + 1)
                .currency(CurrencyType.TRY)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("1000.00"))
                .status(status)
                .lateReturnStatus(LateReturnStatus.ON_TIME)
                .build());
    }

    private static String carFilter(Car car) {
        return "$.content[?(@.id == " + car.getId() + ")]";
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("a picked-up car is IN_USE, a confirmed rental covering today is RESERVED, everything else is FREE")
    void listReportsOccupancy() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars").param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(carFilter(inUse) + ".occupancy").value("IN_USE"))
                .andExpect(jsonPath(carFilter(inUse) + ".currentRentalId").value(inUseRental.getId().intValue()))
                .andExpect(jsonPath(carFilter(reserved) + ".occupancy").value("RESERVED"))
                .andExpect(jsonPath(carFilter(reserved) + ".currentRentalId").value(reservedRental.getId().intValue()))
                .andExpect(jsonPath(carFilter(upcoming) + ".occupancy").value("FREE"))
                .andExpect(jsonPath(carFilter(upcoming) + ".currentRentalId").isEmpty())
                .andExpect(jsonPath(carFilter(free) + ".occupancy").value("FREE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("the single car view carries the same occupancy")
    void detailReportsOccupancy() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars/{id}", inUse.getId()))
                .andExpect(jsonPath("$.occupancy").value("IN_USE"))
                .andExpect(jsonPath("$.currentRentalId").value(inUseRental.getId().intValue()))
                .andExpect(jsonPath("$.carStatusType").value("AVAILABLE"));
        mockMvc.perform(get("/api/v1/admin/cars/{id}", free.getId()))
                .andExpect(jsonPath("$.occupancy").value("FREE"))
                .andExpect(jsonPath("$.currentRentalId").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("the fleet filters by occupancy and combines it with the status filter")
    void filtersByOccupancy() throws Exception {
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "IN_USE"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "RESERVED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(reserved.getId().intValue()));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "FREE"))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "IN_USE").param("status", "AVAILABLE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(inUse.getId().intValue()));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "NOPE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("occupancy agrees with the dashboard fleet status for cars in service")
    void agreesWithDashboard() throws Exception {
        FleetStatusDto fleet = dashboardService.getFleetStatus();

        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "IN_USE").param("status", "AVAILABLE"))
                .andExpect(jsonPath("$.totalElements").value(fleet.rentedCars()));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "RESERVED").param("status", "AVAILABLE"))
                .andExpect(jsonPath("$.totalElements").value(fleet.reservedCars()));
        mockMvc.perform(get("/api/v1/admin/cars").param("occupancy", "FREE").param("status", "AVAILABLE"))
                .andExpect(jsonPath("$.totalElements").value(fleet.availableCars()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("the occupancy is computed in bulk: statements do not grow with the page size")
    void statementCountIsIndependentOfPageSize() throws Exception {
        long small = statementsFor(1);
        long large = statementsFor(5);

        assertThat(large).isEqualTo(small);
        assertThat(small).isLessThanOrEqualTo(4);
    }

    private long statementsFor(int size) throws Exception {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mockMvc.perform(get("/api/v1/admin/cars").param("size", String.valueOf(size)).param("sort", "id,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(size));
        return statistics.getPrepareStatementCount();
    }
}
