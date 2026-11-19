package com.akif.rental.integration;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.internal.service.CarQueryService;
import com.akif.car.api.CarStatusType;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.api.RentalStatus;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Booking integrity")
class BookingIntegrityIntegrationTest {

    private static final int PARALLEL_REQUESTS = 8;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private CarQueryService carQueryService;

    @Autowired
    private JwtTokenService tokenService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final LocalDate start = LocalDate.now().plusDays(20);
    private final LocalDate end = LocalDate.now().plusDays(24);

    @BeforeEach
    void setUp() {
        cleanUp();
    }

    @AfterEach
    void cleanUp() {
        rentalRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM car_rental.payments");
        carRepository.deleteAll();
        userRepository.deleteAll();
    }

    private String tokenFor(User user) {
        return "Bearer " + tokenService.generateAccessToken(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + Role.USER.name()))),
                user.getId());
    }

    private MvcResult book(String bearer, Long carId, LocalDate from, LocalDate to) throws Exception {
        return mockMvc.perform(post("/api/v1/rentals/request")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RentalRequest(carId, from, to, "booking"))))
                .andReturn();
    }

    private String errorCode(MvcResult result) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.path("code").asText();
    }

    @Test
    @DisplayName("date rules come from the service: a past start answers RENTAL_VALIDATION_FAILED")
    void pastStartIsRejectedByTheServiceRule() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("renter")));

        MvcResult result = book(token, car.getId(), LocalDate.now().minusDays(2), LocalDate.now().plusDays(1));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(result)).isEqualTo("RENTAL_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("parallel requests for the same car and dates produce exactly one booking")
    void parallelRequestsProduceExactlyOneBooking() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < PARALLEL_REQUESTS; i++) {
            tokens.add(tokenFor(userRepository.save(TestDataBuilder.createTestUser("racer"))));
        }

        ExecutorService executor = Executors.newFixedThreadPool(PARALLEL_REQUESTS);
        CountDownLatch ready = new CountDownLatch(PARALLEL_REQUESTS);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<MvcResult>> futures = new ArrayList<>();
        try {
            for (String token : tokens) {
                Callable<MvcResult> task = () -> {
                    ready.countDown();
                    go.await();
                    return book(token, car.getId(), start, end);
                };
                futures.add(executor.submit(task));
            }
            ready.await();
            go.countDown();

            int created = 0;
            int conflicts = 0;
            for (Future<MvcResult> future : futures) {
                MvcResult result = future.get();
                int status = result.getResponse().getStatus();
                if (status == 201) {
                    created++;
                } else {
                    assertThat(status).isEqualTo(409);
                    assertThat(errorCode(result)).isEqualTo("CAR_DATES_UNAVAILABLE");
                    conflicts++;
                }
            }

            assertThat(created).isEqualTo(1);
            assertThat(conflicts).isEqualTo(PARALLEL_REQUESTS - 1);
            assertThat(rentalRepository.count()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("a user cannot hold more than three open requests")
    void openRequestCapEnforced() throws Exception {
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("hoarder")));
        for (int i = 0; i < 3; i++) {
            Car car = carRepository.save(TestDataBuilder.createAvailableCar());
            assertThat(book(token, car.getId(), start, end).getResponse().getStatus()).isEqualTo(201);
        }

        Car fourth = carRepository.save(TestDataBuilder.createAvailableCar());
        MvcResult rejected = book(token, fourth.getId(), start, end);

        assertThat(rejected.getResponse().getStatus()).isEqualTo(409);
        assertThat(errorCode(rejected)).isEqualTo("OPEN_RENTAL_REQUEST_LIMIT_EXCEEDED");
        assertThat(rentalRepository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("a rental longer than 30 days is rejected by the rental date rules")
    void overlongRentalRejected() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("long-renter")));

        MvcResult result = book(token, car.getId(), start, start.plusDays(30));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(result)).isEqualTo("RENTAL_VALIDATION_FAILED");
        assertThat(book(token, car.getId(), start, start.plusDays(29)).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("a start date more than 180 days ahead is rejected by the rental date rules")
    void farFutureRentalRejected() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("planner")));
        LocalDate far = LocalDate.now().plusDays(181);

        MvcResult result = book(token, car.getId(), far, far.plusDays(2));

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(result)).isEqualTo("RENTAL_VALIDATION_FAILED");
    }

    @Test
    @DisplayName("an overdue in-use rental is detected as blocking pickup of the next rental on the car")
    void overdueInUseRentalBlocksNextPickup() {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        User renter = userRepository.save(TestDataBuilder.createTestUser("overdue"));
        LocalDate today = LocalDate.now();
        var overdue = rentalRepository.save(TestDataBuilder.createRental(
                car, renter, today.minusDays(6), today.minusDays(2), RentalStatus.IN_USE));
        var next = rentalRepository.save(TestDataBuilder.createRental(
                car, renter, today, today.plusDays(3), RentalStatus.CONFIRMED));

        assertThat(rentalRepository.existsByCarIdAndStatusAndIsDeletedFalseAndIdNot(
                car.getId(), RentalStatus.IN_USE, next.getId())).isTrue();
        assertThat(rentalRepository.existsByCarIdAndStatusAndIsDeletedFalseAndIdNot(
                car.getId(), RentalStatus.IN_USE, overdue.getId())).isFalse();
    }

    @Test
    @DisplayName("overlapping dates are rejected with 409 and the car-not-available code")
    void overlappingDatesRejected() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String first = tokenFor(userRepository.save(TestDataBuilder.createTestUser("first")));
        String second = tokenFor(userRepository.save(TestDataBuilder.createTestUser("second")));

        assertThat(book(first, car.getId(), start, end).getResponse().getStatus()).isEqualTo(201);

        MvcResult overlapping = book(second, car.getId(), end, end.plusDays(2));
        assertThat(overlapping.getResponse().getStatus()).isEqualTo(409);
        assertThat(errorCode(overlapping)).isEqualTo("CAR_DATES_UNAVAILABLE");

        assertThat(book(second, car.getId(), end.plusDays(1), end.plusDays(3)).getResponse().getStatus())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("booking never changes the fleet status of the car")
    void bookingDoesNotChangeFleetStatus() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("renter")));

        MvcResult result = book(token, car.getId(), start, end);
        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        assertThat(carRepository.findById(car.getId()).orElseThrow().getCarStatusType())
                .isEqualTo(CarStatusType.AVAILABLE);
    }

    @Test
    @DisplayName("a car with a booking stays bookable for other dates")
    void carStaysBookableForOtherDates() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("renter")));

        assertThat(book(token, car.getId(), start, end).getResponse().getStatus()).isEqualTo(201);
        assertThat(book(token, car.getId(), end.plusDays(5), end.plusDays(8)).getResponse().getStatus())
                .isEqualTo(201);
    }

    @Test
    @DisplayName("a cancelled booking frees the dates for another renter")
    void cancelledBookingFreesDates() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        User owner = userRepository.save(TestDataBuilder.createTestUser("owner"));
        String ownerToken = tokenFor(owner);
        String otherToken = tokenFor(userRepository.save(TestDataBuilder.createTestUser("other")));

        MvcResult created = book(ownerToken, car.getId(), start, end);
        long rentalId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/v1/rentals/{id}/cancel", rentalId)
                        .header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"no longer needed\"}"))
                .andReturn();

        assertThat(rentalRepository.findById(rentalId).orElseThrow().getStatus()).isEqualTo(RentalStatus.CANCELLED);
        assertThat(book(otherToken, car.getId(), start, end).getResponse().getStatus()).isEqualTo(201);
    }

    @Test
    @DisplayName("a car in maintenance is refused with 400 and the car-not-available code")
    void maintenanceCarRefused() throws Exception {
        Car car = TestDataBuilder.createAvailableCar();
        car.setCarStatusType(CarStatusType.MAINTENANCE);
        car = carRepository.save(car);
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("renter")));

        MvcResult result = book(token, car.getId(), start, end);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(result)).isEqualTo("CAR_NOT_AVAILABLE");
    }

    @Test
    @DisplayName("the booking path ignores a stale cached car")
    void bookingPathBypassesCache() throws Exception {
        Car car = carRepository.save(TestDataBuilder.createAvailableCar());
        String token = tokenFor(userRepository.save(TestDataBuilder.createTestUser("renter")));

        assertThat(carQueryService.getCarById(car.getId()).carStatusType()).isEqualTo(CarStatusType.AVAILABLE);
        jdbcTemplate.update("UPDATE car_rental.car SET car_status_type = 'MAINTENANCE' WHERE id = ?", car.getId());
        assertThat(carQueryService.getCarById(car.getId()).carStatusType())
                .as("the cached read is stale, which is the point of the test")
                .isEqualTo(CarStatusType.AVAILABLE);

        MvcResult result = book(token, car.getId(), start, end);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(errorCode(result)).isEqualTo("CAR_NOT_AVAILABLE");
        assertThat(rentalRepository.count()).isZero();
    }
}
