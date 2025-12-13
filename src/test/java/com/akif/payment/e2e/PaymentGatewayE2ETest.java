package com.akif.payment.e2e;

import com.akif.auth.domain.User;
import com.akif.auth.internal.repository.UserRepository;
import com.akif.car.domain.Car;
import com.akif.car.internal.repository.CarRepository;
import com.akif.e2e.infrastructure.E2ETestBase;
import com.akif.e2e.infrastructure.TestDataBuilder;
import com.akif.payment.api.PaymentResult;
import com.akif.payment.domain.Payment;
import com.akif.payment.api.PaymentStatus;
import com.akif.payment.internal.repository.PaymentRepository;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.starter.CarGalleryProjectApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = CarGalleryProjectApplication.class)
@DisplayName("Payment Gateway E2E Tests")
class PaymentGatewayE2ETest extends E2ETestBase {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoSpyBean
    private PaymentGateway paymentGateway;

    @Nested
    @DisplayName("Payment Authorization Tests")
    class PaymentAuthorizationTests {

        @Test
        @DisplayName("Should call StubPaymentGateway.authorize and set payment status to AUTHORIZED when rental is confirmed")
        void shouldAuthorizePaymentOnConfirm() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CONFIRMED.getDisplayName()));

            verify(paymentGateway, times(1)).authorize(any(), any(), anyString());

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
            assertThat(payment.getTransactionId()).isNotNull();
            assertThat(payment.getTransactionId()).startsWith("STUB-");
            assertThat(payment.getAmount()).isNotNull();
            assertThat(payment.getCurrency()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Payment Capture Tests")
    class PaymentCaptureTests {

        @Test
        @DisplayName("Should call StubPaymentGateway.capture and set payment status to CAPTURED when rental is picked up")
        void shouldCapturePaymentOnPickup() throws Exception {

            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);


            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            reset(paymentGateway);

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.IN_USE.getDisplayName()));

            verify(paymentGateway, times(1)).capture(anyString(), any());

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CAPTURED);
            assertThat(payment.getTransactionId()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Payment Refund Tests")
    class PaymentRefundTests {

        @Test
        @DisplayName("Should call StubPaymentGateway.refund and set payment status to REFUNDED when IN_USE rental is cancelled")
        void shouldRefundPaymentOnCancel() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/rentals/{id}/pickup", rentalId)
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());

            reset(paymentGateway);

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            verify(paymentGateway, times(1)).refund(anyString(), any());

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }

        @Test
        @DisplayName("Should set payment status to REFUNDED without calling gateway when CONFIRMED rental is cancelled")
        void shouldRefundAuthorizedPaymentOnCancel() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isOk());

            reset(paymentGateway);

            mockMvc.perform(post("/api/rentals/{id}/cancel", rentalId)
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(rentalId))
                    .andExpect(jsonPath("$.status").value(RentalStatus.CANCELLED.getDisplayName()));

            verify(paymentGateway, never()).refund(anyString(), any());

            Payment payment = paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId)
                    .orElseThrow(() -> new AssertionError("Payment not found for rental: " + rentalId));
            
            assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        }
    }

    @Nested
    @DisplayName("Payment Failure Handling Tests")
    class PaymentFailureHandlingTests {

        @Test
        @DisplayName("Should keep rental in REQUESTED state when payment authorization fails")
        void shouldKeepRentalRequestedOnAuthorizationFailure() throws Exception {
            User testUser = TestDataBuilder.createTestUser();
            testUser = userRepository.save(testUser);
            String userToken = generateUserToken(testUser);

            User testAdmin = TestDataBuilder.createTestAdmin();
            testAdmin = userRepository.save(testAdmin);
            String adminToken = generateAdminToken(testAdmin);

            Car testCar = TestDataBuilder.createAvailableCar();
            testCar = carRepository.save(testCar);

            RentalRequest rentalRequest = TestDataBuilder.createRentalRequest(testCar.getId());
            Long rentalId = createAndGetRentalId(rentalRequest, userToken);

            doReturn(PaymentResult.failure("Authorization failed - insufficient funds"))
                    .when(paymentGateway).authorize(any(), any(), anyString());

            mockMvc.perform(post("/api/rentals/{id}/confirm", rentalId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isPaymentRequired())
                    .andExpect(jsonPath("$.errorCode").value("PAYMENT_FAILED"));

            mockMvc.perform(post("/api/rentals/request")
                            .header("Authorization", "Bearer " + userToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rentalRequest)))
                    .andExpect(status().isCreated());

            paymentRepository.findByRentalIdAndIsDeletedFalse(rentalId).ifPresent(payment -> assertThat(payment.getStatus()).isNotEqualTo(PaymentStatus.AUTHORIZED));

        }
    }
}
