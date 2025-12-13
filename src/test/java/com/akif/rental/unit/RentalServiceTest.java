package com.akif.rental.unit;

import com.akif.auth.api.AuthService;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarResponse;
import com.akif.car.api.CarService;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.car.internal.dto.pricing.PricingResult;
import com.akif.car.internal.exception.CarNotAvailableException;
import com.akif.car.internal.service.pricing.DynamicPricingService;
import com.akif.payment.api.*;
import com.akif.payment.internal.exception.PaymentFailedException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.rental.internal.exception.*;
import com.akif.rental.internal.mapper.RentalMapper;
import com.akif.rental.internal.repository.RentalRepository;
import com.akif.rental.internal.service.RentalServiceImpl;
import com.akif.rental.internal.service.penalty.PenaltyCalculationService;
import com.akif.rental.internal.service.penalty.PenaltyPaymentService;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.akif.shared.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalService Unit Tests")
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private CarService carService;

    @Mock
    private AuthService authService;

    @Mock
    private PaymentService paymentService;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private DynamicPricingService dynamicPricingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PenaltyCalculationService penaltyCalculationService;

    @Mock
    private PenaltyPaymentService penaltyPaymentService;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private UserDto testUser;
    private CarResponse testCar;
    private RentalRequest rentalRequest;
    private Rental testRental;
    private RentalResponse testRentalResponse;
    private PricingResult testPricingResult;

    @BeforeEach
    void setUp() {
        testUser = new UserDto(
                1L,
                "testuser",
                "test@example.com",
                "Test",
                "User",
                Set.of(Role.USER),
                true
        );

        testCar = CarResponse.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        rentalRequest = new RentalRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5),
                "Test rental request"
        );

        testRental = Rental.builder()
                .id(1L)
                .userId(testUser.id())
                .userEmail(testUser.email())
                .userFullName(testUser.firstName() + " " + testUser.lastName())
                .carId(testCar.getId())
                .carBrand(testCar.getBrand())
                .carModel(testCar.getModel())
                .carLicensePlate(testCar.getLicensePlate())
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .days(5)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("2500.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.REQUESTED)
                .build();

        testPricingResult = PricingResult.builder()
                .basePrice(new BigDecimal("500.00"))
                .rentalDays(5)
                .appliedModifiers(Collections.emptyList())
                .combinedMultiplier(BigDecimal.ONE)
                .finalPrice(new BigDecimal("2500.00"))
                .build();

        testRentalResponse = createTestRentalResponse(testRental);
    }

    private RentalResponse createTestRentalResponse(Rental rental) {
        return new RentalResponse(
                rental.getId(),
                null, // carSummary
                null, // userSummary
                rental.getStartDate(),
                rental.getEndDate(),
                rental.getDays(),
                rental.getDailyPrice(),
                rental.getTotalPrice(),
                rental.getCurrency(),
                rental.getStatus(),
                rental.getTotalPrice(),
                rental.getTotalPrice(),
                BigDecimal.ZERO,
                Collections.emptyList(),
                null, null, null, null,
                rental.getPickupNotes(),
                rental.getReturnNotes(),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("Request Rental Tests")
    class RequestRentalTests {

        @Test
        @DisplayName("Should successfully create rental request")
        void shouldSuccessfullyCreateRentalRequest() {
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarById(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(testPricingResult);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.requestRental(rentalRequest, "testuser");

            assertThat(result).isNotNull();
            verify(authService).getUserByUsername("testuser");
            verify(carService).getCarById(1L);
            verify(rentalRepository).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should throw exception when car is not available")
        void shouldThrowExceptionWhenCarNotAvailable() {
            CarResponse reservedCar = CarResponse.builder()
                    .id(1L)
                    .carStatusType(CarStatusType.RESERVED)
                    .build();

            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarById(1L)).thenReturn(reservedCar);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarNotAvailableException.class);

            verify(rentalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when date overlap exists")
        void shouldThrowExceptionWhenDateOverlapExists() {
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarById(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(1L);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(RentalDateOverlapException.class);

            verify(rentalRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when start date is in past")
        void shouldThrowExceptionWhenStartDateIsInPast() {
            RentalRequest pastRequest = new RentalRequest(
                    1L,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(5),
                    null
            );

            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarById(1L)).thenReturn(testCar);

            assertThatThrownBy(() -> rentalService.requestRental(pastRequest, "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("past");
        }
    }

    @Nested
    @DisplayName("Confirm Rental Tests")
    class ConfirmRentalTests {

        @Test
        @DisplayName("Should successfully confirm rental and authorize payment")
        void shouldSuccessfullyConfirmRentalAndAuthorizePayment() {
            PaymentResult successResult = new PaymentResult(true, "TXN-123", "Success");
            PaymentDto paymentDto = createTestPaymentDto();

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(paymentService.authorize(any(), any(), any())).thenReturn(successResult);
            when(paymentService.createPayment(any())).thenReturn(paymentDto);
            when(paymentService.updatePaymentStatus(anyLong(), any(), any(), any())).thenReturn(paymentDto);
            when(carService.reserveCar(anyLong())).thenReturn(testCar);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.confirmRental(1L);

            assertThat(result).isNotNull();
            verify(paymentService).authorize(any(), any(), any());
            verify(paymentService).createPayment(any());
            verify(carService).reserveCar(1L);
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when payment authorization fails")
        void shouldThrowExceptionWhenPaymentAuthorizationFails() {
            PaymentResult failedResult = new PaymentResult(false, null, "Card declined");

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(paymentService.authorize(any(), any(), any())).thenReturn(failedResult);

            assertThatThrownBy(() -> rentalService.confirmRental(1L))
                    .isInstanceOf(PaymentFailedException.class);

            verify(carService, never()).reserveCar(anyLong());
        }
    }

    @Nested
    @DisplayName("Cancel Rental Tests")
    class CancelRentalTests {

        @Test
        @DisplayName("Should successfully cancel requested rental without refund")
        void shouldSuccessfullyCancelRequestedRentalWithoutRefund() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(carService.getCarById(1L)).thenReturn(testCar);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.cancelRental(1L, "testuser");

            assertThat(result).isNotNull();
            verify(paymentService, never()).refundPayment(anyLong(), any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should throw exception when user tries to cancel other user's rental")
        void shouldThrowExceptionWhenUserTriesToCancelOtherUsersRental() {
            UserDto otherUser = new UserDto(2L, "otheruser", "other@example.com", "Other", "User", Set.of(Role.USER), true);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(authService.getUserByUsername("otheruser")).thenReturn(otherUser);

            assertThatThrownBy(() -> rentalService.cancelRental(1L, "otheruser"))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Pickup and Return Tests")
    class PickupAndReturnTests {

        @Test
        @DisplayName("Should successfully process pickup and capture payment")
        void shouldSuccessfullyProcessPickupAndCapturePayment() {
            testRental.updateStatus(RentalStatus.CONFIRMED);
            PaymentDto paymentDto = createTestPaymentDto();
            PaymentResult captureResult = new PaymentResult(true, "TXN-123", "Captured");

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(paymentDto));
            when(paymentService.capture(any(), any())).thenReturn(captureResult);
            when(paymentService.updatePaymentStatus(anyLong(), any(), any(), any())).thenReturn(paymentDto);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.pickupRental(1L, "Pickup notes");

            assertThat(result).isNotNull();
            verify(paymentService).capture(any(), any());
            verify(eventPublisher).publishEvent(any());
        }

        @Test
        @DisplayName("Should successfully process return")
        void shouldSuccessfullyProcessReturn() {
            testRental.updateStatus(RentalStatus.IN_USE);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(carService.releaseCar(1L)).thenReturn(testCar);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.returnRental(1L, "Return notes");

            assertThat(result).isNotNull();
            verify(carService).releaseCar(1L);
        }
    }

    @Nested
    @DisplayName("Get Rentals Tests")
    class GetRentalsTests {

        @Test
        @DisplayName("Should return user's rentals")
        void shouldReturnUsersRentals() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> rentalsPage = new PageImpl<>(List.of(testRental));

            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(rentalRepository.findByUserIdAndIsDeletedFalse(testUser.id(), pageable)).thenReturn(rentalsPage);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            Page<RentalResponse> result = rentalService.getMyRentals("testuser", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty page when user has no rentals")
        void shouldReturnEmptyPageWhenUserHasNoRentals() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> emptyPage = Page.empty();

            when(authService.getUserByUsername("testuser")).thenReturn(testUser);
            when(rentalRepository.findByUserIdAndIsDeletedFalse(testUser.id(), pageable)).thenReturn(emptyPage);

            Page<RentalResponse> result = rentalService.getMyRentals("testuser", pageable);

            assertThat(result).isEmpty();
        }
    }

    private PaymentDto createTestPaymentDto() {
        return new PaymentDto(
                1L,
                testRental.getId(),
                testUser.email(),
                testCar.getLicensePlate(),
                testRental.getTotalPrice(),
                testRental.getCurrency(),
                PaymentStatus.AUTHORIZED,
                "STUB_GATEWAY",
                "TXN-123",
                null,
                null,
                null
        );
    }
}
