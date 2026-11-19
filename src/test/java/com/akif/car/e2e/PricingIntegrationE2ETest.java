package com.akif.car.e2e;

import com.akif.CarRentalApplication;
import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.rental.api.RentalRequest;
import com.akif.rental.api.RentalResponse;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@DisplayName("Dynamic Pricing Integration E2E Tests")
class PricingIntegrationE2ETest extends E2ETestBase {

    private static final Instant SATURDAY_IN_MARCH = Instant.parse("2031-03-08T10:00:00Z");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    private String userToken;
    private Car car;

    @BeforeEach
    void pinTheCalendar() {
        clock.pinTo(SATURDAY_IN_MARCH);
        User user = userRepository.save(TestDataBuilder.createTestUser());
        userToken = generateUserToken(user);
        car = carRepository.save(TestDataBuilder.createAvailableCar());
    }

    private RentalResponse request(LocalDate startDate, LocalDate endDate) throws Exception {
        RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(car.getId(), startDate, endDate);
        String responseJson = mockMvc.perform(post("/api/v1/rentals/request")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rentalRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(responseJson, RentalResponse.class);
    }

    @Nested
    @DisplayName("Early Booking Discount Tests")
    class EarlyBookingDiscountTests {

        @Test
        @DisplayName("Should apply 15% early booking discount when rental is requested 30+ days in advance")
        void shouldApplyEarlyBookingDiscount_whenBooking30DaysInAdvance() throws Exception {
            RentalResponse response = request(LocalDate.of(2031, 4, 14), LocalDate.of(2031, 4, 17));

            assertThat(response.appliedDiscounts()).anyMatch(text -> text.contains("Early booking discount (30+ days advance)"));
            assertThat(response.originalPrice()).isEqualByComparingTo("2000.00");
            assertThat(response.finalPrice()).isEqualByComparingTo("1700.00");
            assertThat(response.totalSavings()).isEqualByComparingTo("300.00");
            assertThat(response.totalPrice()).isEqualByComparingTo("1700.00");
        }
    }

    @Nested
    @DisplayName("Duration Discount Tests")
    class DurationDiscountTests {

        @Test
        @DisplayName("Should apply duration discount when rental is requested for 14+ days")
        void shouldApplyDurationDiscount_whenRentalDuration14Days() throws Exception {
            RentalResponse response = request(LocalDate.of(2031, 3, 10), LocalDate.of(2031, 3, 23));

            assertThat(response.days()).isEqualTo(14);
            assertThat(response.appliedDiscounts()).anyMatch(text -> text.toLowerCase().contains("rental discount"));
            assertThat(response.originalPrice()).isEqualByComparingTo("7000.00");
            assertThat(response.finalPrice()).isEqualByComparingTo("6332.59");
            assertThat(response.totalSavings()).isEqualByComparingTo("667.41");
        }
    }

    @Nested
    @DisplayName("Weekend Surcharge Tests")
    class WeekendSurchargeTests {

        @Test
        @DisplayName("Should apply weekend surcharge when rental includes weekend days")
        void shouldApplyWeekendSurcharge_whenRentalIncludesWeekend() throws Exception {
            RentalResponse response = request(LocalDate.of(2031, 3, 8), LocalDate.of(2031, 3, 10));

            assertThat(response.appliedDiscounts()).anyMatch(text -> text.contains("Weekend pricing (2/3 days)"));
            assertThat(response.originalPrice()).isEqualByComparingTo("1500.00");
            assertThat(response.finalPrice()).isEqualByComparingTo("1650.00");
            assertThat(response.totalPrice()).isEqualByComparingTo("1650.00");
        }
    }

    @Nested
    @DisplayName("Price Breakdown Verification Tests")
    class PriceBreakdownVerificationTests {

        @Test
        @DisplayName("Should include every strategy in the price breakdown and price a plain weekday rental at the base rate")
        void shouldIncludeAllAppliedModifiersInPriceBreakdown() throws Exception {
            RentalResponse response = request(LocalDate.of(2031, 3, 11), LocalDate.of(2031, 3, 13));

            assertThat(response.appliedDiscounts()).containsExactlyInAnyOrder(
                    "Regular season",
                    "No early booking discount",
                    "No duration discount",
                    "No weekend days",
                    "Normal demand (0% occupancy)");
            assertThat(response.days()).isEqualTo(3);
            assertThat(response.dailyPrice()).isEqualByComparingTo("500.00");
            assertThat(response.originalPrice()).isEqualByComparingTo("1500.00");
            assertThat(response.finalPrice()).isEqualByComparingTo("1500.00");
            assertThat(response.totalSavings()).isEqualByComparingTo("0.00");
            assertThat(response.totalPrice()).isEqualByComparingTo("1500.00");
        }
    }
}
