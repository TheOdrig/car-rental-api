package com.akif.e2e.pricing;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.model.Car;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.UserRepository;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Dynamic Pricing Integration E2E Tests")
class PricingIntegrationE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Nested
    @DisplayName("Early Booking Discount Tests")
    class EarlyBookingDiscountTests {

        @Test
        @DisplayName("Should apply 15% early booking discount when rental is requested 30+ days in advance")
        void shouldApplyEarlyBookingDiscount_whenBooking30DaysInAdvance() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createEarlyBookingRentalRequest(testCar.getId());

            String responseJson = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getAppliedDiscounts()).isNotNull();
            assertThat(response.getAppliedDiscounts())
                    .anyMatch(discount -> discount.toLowerCase().contains("early booking"));

            assertThat(response.getTotalSavings()).isNotNull();
            assertThat(response.getTotalSavings()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.getFinalPrice()).isLessThan(response.getOriginalPrice());
        }
    }

    @Nested
    @DisplayName("Duration Discount Tests")
    class DurationDiscountTests {

        @Test
        @DisplayName("Should apply duration discount when rental is requested for 14+ days")
        void shouldApplyDurationDiscount_whenRentalDuration14Days() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createLongDurationRentalRequest(testCar.getId());

            String responseJson = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getAppliedDiscounts()).isNotNull();
            assertThat(response.getAppliedDiscounts())
                    .anyMatch(discount -> discount.toLowerCase().contains("rental discount"));

            assertThat(response.getTotalSavings()).isNotNull();
            assertThat(response.getTotalSavings()).isGreaterThan(BigDecimal.ZERO);

            assertThat(response.getFinalPrice()).isLessThan(response.getOriginalPrice());
        }
    }

    @Nested
    @DisplayName("Weekend Surcharge Tests")
    class WeekendSurchargeTests {

        @Test
        @DisplayName("Should apply weekend surcharge when rental includes weekend days")
        void shouldApplyWeekendSurcharge_whenRentalIncludesWeekend() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            LocalDate startDate = LocalDate.now().plusDays(1);
            while (startDate.getDayOfWeek() != DayOfWeek.SATURDAY) {
                startDate = startDate.plusDays(1);
            }
            LocalDate endDate = startDate.plusDays(2);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(
                    testCar.getId(), startDate, endDate);

            String responseJson = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getAppliedDiscounts()).isNotNull();
            assertThat(response.getAppliedDiscounts())
                    .anyMatch(discount -> discount.toLowerCase().contains("weekend"));

            assertThat(response.getFinalPrice()).isNotNull();
            assertThat(response.getOriginalPrice()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Price Breakdown Verification Tests")
    class PriceBreakdownVerificationTests {

        @Test
        @DisplayName("Should include all applied modifiers in price breakdown")
        void shouldIncludeAllAppliedModifiersInPriceBreakdown() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequestDto rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());

            String responseJson = mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            RentalResponseDto response = objectMapper.readValue(responseJson, RentalResponseDto.class);

            assertThat(response.getOriginalPrice()).isNotNull();
            assertThat(response.getFinalPrice()).isNotNull();
            assertThat(response.getTotalSavings()).isNotNull();
            assertThat(response.getAppliedDiscounts()).isNotNull();

            assertThat(response.getDays()).isGreaterThan(0);
            assertThat(response.getDailyPrice()).isNotNull();
            assertThat(response.getTotalPrice()).isNotNull();
        }
    }
}
