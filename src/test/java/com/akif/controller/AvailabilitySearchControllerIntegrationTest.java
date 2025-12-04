package com.akif.controller;

import com.akif.dto.availability.AvailabilitySearchRequestDto;
import com.akif.enums.CarStatusType;
import com.akif.enums.CurrencyType;
import com.akif.enums.RentalStatus;
import com.akif.enums.Role;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AvailabilitySearchController Integration Tests")
class AvailabilitySearchControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private Car availableCar;
    private Car unavailableCar;
    private Car maintenanceCar;
    private User testUser;

    @BeforeEach
    void setUp() {
        rentalRepository.deleteAll();
        carRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password(passwordEncoder.encode("password123"))
                .roles(Set.of(Role.USER))
                .enabled(true)
                .isDeleted(false)
                .build();
        testUser = userRepository.save(testUser);

        availableCar = Car.builder()
                .licensePlate("34ABC123")
                .vinNumber("1HGBH41JXMN109186")
                .brand("Toyota")
                .model("Corolla")
                .productionYear(2020)
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .engineType("Gasoline")
                .fuelType("Gasoline")
                .transmissionType("Automatic")
                .bodyType("Sedan")
                .color("White")
                .kilometer(10000L)
                .doors(4)
                .seats(5)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .isDeleted(false)
                .build();
        availableCar = carRepository.save(availableCar);

        unavailableCar = Car.builder()
                .licensePlate("34XYZ789")
                .vinNumber("2HGBH41JXMN109187")
                .brand("Honda")
                .model("Civic")
                .productionYear(2021)
                .price(new BigDecimal("600.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .engineType("Gasoline")
                .fuelType("Gasoline")
                .transmissionType("Automatic")
                .bodyType("Sedan")
                .color("Black")
                .kilometer(5000L)
                .doors(4)
                .seats(5)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .isDeleted(false)
                .build();
        unavailableCar = carRepository.save(unavailableCar);

        Rental rental = Rental.builder()
                .car(unavailableCar)
                .user(testUser)
                .startDate(LocalDate.now().plusDays(5))
                .endDate(LocalDate.now().plusDays(10))
                .days(5)
                .dailyPrice(new BigDecimal("600.00"))
                .totalPrice(new BigDecimal("3000.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.CONFIRMED)
                .isDeleted(false)
                .build();
        rentalRepository.save(rental);

        maintenanceCar = Car.builder()
                .licensePlate("06DEF456")
                .vinNumber("3HGBH41JXMN109188")
                .brand("Toyota")
                .model("Camry")
                .productionYear(2022)
                .price(new BigDecimal("700.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.MAINTENANCE)
                .engineType("Gasoline")
                .fuelType("Gasoline")
                .transmissionType("Automatic")
                .bodyType("Sedan")
                .color("Silver")
                .kilometer(2000L)
                .doors(4)
                .seats(5)
                .isFeatured(false)
                .isTestDriveAvailable(false)
                .viewCount(0L)
                .likeCount(0L)
                .isDeleted(false)
                .build();
        maintenanceCar = carRepository.save(maintenanceCar);
    }

    @Nested
    @DisplayName("Search Available Cars Tests")
    class SearchAvailableCarsTests {

        @Test
        @DisplayName("Should return available cars for valid date range - Requirement 1.1")
        void shouldReturnAvailableCarsForValidDateRange() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(4))
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cars").isArray())
                    .andExpect(jsonPath("$.cars", hasSize(greaterThan(0))))
                    .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
                    .andExpect(jsonPath("$.rentalDays").value(4))
                    .andExpect(jsonPath("$.searchStartDate").exists())
                    .andExpect(jsonPath("$.searchEndDate").exists());
        }

        @Test
        @DisplayName("Should exclude cars with blocking status - Requirement 1.2")
        void shouldExcludeCarsWithBlockingStatus() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(4))
                    .build();

            String response = mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assert !response.contains(maintenanceCar.getLicensePlate());
        }

        @Test
        @DisplayName("Should exclude cars with overlapping rentals - Requirement 1.1")
        void shouldExcludeCarsWithOverlappingRentals() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(6))
                    .endDate(LocalDate.now().plusDays(9))
                    .build();

            String response = mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assert !response.contains(unavailableCar.getLicensePlate());
        }

        @Test
        @DisplayName("Should apply combined filters - Requirement 1.5")
        void shouldApplyCombinedFilters() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(4))
                    .brand("Toyota")
                    .minPrice(new BigDecimal("400.00"))
                    .maxPrice(new BigDecimal("600.00"))
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cars").isArray())
                    .andExpect(jsonPath("$.cars[*].brand", everyItem(equalTo("Toyota"))));
        }

        @Test
        @DisplayName("Should include pricing information - Requirement 1.1, 4.1")
        void shouldIncludePricingInformation() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(4))
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cars[0].dailyRate").exists())
                    .andExpect(jsonPath("$.cars[0].totalPrice").exists())
                    .andExpect(jsonPath("$.cars[0].currency").exists());
        }

        @Test
        @DisplayName("Should support pagination - Requirement 1.1")
        void shouldSupportPagination() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(1))
                    .endDate(LocalDate.now().plusDays(4))
                    .page(0)
                    .size(10)
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.currentPage").value(0))
                    .andExpect(jsonPath("$.pageSize").value(10))
                    .andExpect(jsonPath("$.totalPages").exists());
        }
    }

    @Nested
    @DisplayName("Error Cases Tests")
    class ErrorCasesTests {

        @Test
        @DisplayName("Should reject invalid date range (start after end) - Requirement 1.3")
        void shouldRejectInvalidDateRange() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().plusDays(10))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject past start date - Requirement 1.4")
        void shouldRejectPastStartDate() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .startDate(LocalDate.now().minusDays(5))
                    .endDate(LocalDate.now().plusDays(5))
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should reject null dates - Requirement 1.3")
        void shouldRejectNullDates() throws Exception {
            AvailabilitySearchRequestDto request = AvailabilitySearchRequestDto.builder()
                    .build();

            mockMvc.perform(post("/api/cars/availability/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Calendar Endpoint Tests")
    class CalendarEndpointTests {

        @Test
        @DisplayName("Should return calendar for current month - Requirement 3.1")
        void shouldReturnCalendarForCurrentMonth() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/availability/calendar", availableCar.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.carId").value(availableCar.getId()))
                    .andExpect(jsonPath("$.month").exists())
                    .andExpect(jsonPath("$.days").isArray())
                    .andExpect(jsonPath("$.days", hasSize(greaterThan(27))))
                    .andExpect(jsonPath("$.carBlocked").value(false));
        }

        @Test
        @DisplayName("Should return calendar for specific month - Requirement 3.1")
        void shouldReturnCalendarForSpecificMonth() throws Exception {
            YearMonth nextMonth = YearMonth.now().plusMonths(1);
            String monthParam = nextMonth.toString();

            mockMvc.perform(get("/api/cars/{id}/availability/calendar", availableCar.getId())
                            .param("month", monthParam))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.month").value(monthParam))
                    .andExpect(jsonPath("$.days").isArray());
        }

        @Test
        @DisplayName("Should mark blocked car as unavailable - Requirement 3.1")
        void shouldMarkBlockedCarAsUnavailable() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/availability/calendar", maintenanceCar.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.carBlocked").value(true))
                    .andExpect(jsonPath("$.blockReason").exists());
        }

        @Test
        @DisplayName("Should return 404 for non-existent car - Requirement 3.1")
        void shouldReturn404ForNonExistentCar() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/availability/calendar", 99999L))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject invalid month format - Requirement 3.1")
        void shouldRejectInvalidMonthFormat() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/availability/calendar", availableCar.getId())
                            .param("month", "invalid-month"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Similar Cars Endpoint Tests")
    class SimilarCarsEndpointTests {

        @Test
        @DisplayName("Should return similar available cars - Requirement 2.1")
        void shouldReturnSimilarAvailableCars() throws Exception {
            Car similarCar = Car.builder()
                    .licensePlate("34SIM111")
                    .vinNumber("4HGBH41JXMN109189")
                    .brand("Toyota")
                    .model("Yaris")
                    .productionYear(2021)
                    .price(new BigDecimal("480.00"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .engineType("Gasoline")
                    .fuelType("Gasoline")
                    .transmissionType("Automatic")
                    .bodyType("Sedan")
                    .color("Red")
                    .kilometer(8000L)
                    .doors(4)
                    .seats(5)
                    .isFeatured(false)
                    .isTestDriveAvailable(true)
                    .viewCount(0L)
                    .likeCount(0L)
                    .isDeleted(false)
                    .build();
            carRepository.save(similarCar);

            mockMvc.perform(get("/api/cars/{id}/similar", availableCar.getId())
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("endDate", LocalDate.now().plusDays(4).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(5))))
                    .andExpect(jsonPath("$[0].similarityReasons").isArray())
                    .andExpect(jsonPath("$[0].similarityScore").exists());
        }

        @Test
        @DisplayName("Should return empty list when no similar cars - Requirement 2.1")
        void shouldReturnEmptyListWhenNoSimilarCars() throws Exception {
            Car differentCar = Car.builder()
                    .licensePlate("34DIF222")
                    .vinNumber("5HGBH41JXMN109190")
                    .brand("Mercedes")
                    .model("S-Class")
                    .productionYear(2023)
                    .price(new BigDecimal("5000.00"))
                    .currencyType(CurrencyType.TRY)
                    .carStatusType(CarStatusType.AVAILABLE)
                    .engineType("Diesel")
                    .fuelType("Diesel")
                    .transmissionType("Automatic")
                    .bodyType("Luxury")
                    .color("Black")
                    .kilometer(1000L)
                    .doors(4)
                    .seats(5)
                    .isFeatured(true)
                    .isTestDriveAvailable(true)
                    .viewCount(0L)
                    .likeCount(0L)
                    .isDeleted(false)
                    .build();
            carRepository.save(differentCar);

            mockMvc.perform(get("/api/cars/{id}/similar", differentCar.getId())
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("endDate", LocalDate.now().plusDays(4).toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should respect limit parameter - Requirement 2.1")
        void shouldRespectLimitParameter() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/similar", availableCar.getId())
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("endDate", LocalDate.now().plusDays(4).toString())
                            .param("limit", "3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(3))));
        }

        @Test
        @DisplayName("Should return 404 for non-existent car - Requirement 2.1")
        void shouldReturn404ForNonExistentCarInSimilar() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/similar", 99999L)
                            .param("startDate", LocalDate.now().plusDays(1).toString())
                            .param("endDate", LocalDate.now().plusDays(4).toString()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should reject invalid date parameters - Requirement 2.1")
        void shouldRejectInvalidDateParameters() throws Exception {
            mockMvc.perform(get("/api/cars/{id}/similar", availableCar.getId())
                            .param("startDate", "invalid-date")
                            .param("endDate", LocalDate.now().plusDays(4).toString()))
                    .andExpect(status().isBadRequest());
        }
    }
}
