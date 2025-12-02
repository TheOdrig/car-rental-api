package com.akif.service;

import com.akif.dto.request.RentalRequestDto;
import com.akif.dto.response.RentalResponseDto;
import com.akif.enums.*;
import com.akif.exception.*;
import com.akif.mapper.RentalMapper;
import com.akif.model.Car;
import com.akif.model.Payment;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.PaymentRepository;
import com.akif.repository.RentalRepository;
import com.akif.repository.UserRepository;
import com.akif.service.impl.RentalServiceImpl;
import com.akif.service.gateway.IPaymentGateway;
import com.akif.service.gateway.PaymentResult;
import com.akif.service.pricing.IDynamicPricingService;
import com.akif.service.pricing.PricingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;


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
    private CarRepository carRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private IPaymentGateway paymentGateway;

    @Mock
    private RentalMapper rentalMapper;

    @Mock
    private IDynamicPricingService dynamicPricingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private RentalServiceImpl rentalService;

    private User testUser;
    private Car testCar;
    private RentalRequestDto rentalRequest;
    private Rental testRental;
    private Payment testPayment;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        testCar = Car.builder()
                .id(1L)
                .licensePlate("34ABC123")
                .brand("Toyota")
                .model("Corolla")
                .price(new BigDecimal("500.00"))
                .currencyType(CurrencyType.TRY)
                .carStatusType(CarStatusType.AVAILABLE)
                .build();

        rentalRequest = RentalRequestDto.builder()
                .carId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .notes("Test rental request")
                .build();

        testRental = Rental.builder()
                .id(1L)
                .user(testUser)
                .car(testCar)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .days(5)
                .dailyPrice(new BigDecimal("500.00"))
                .totalPrice(new BigDecimal("2500.00"))
                .currency(CurrencyType.TRY)
                .status(RentalStatus.REQUESTED)
                .build();

        testPayment = Payment.builder()
                .id(1L)
                .rental(testRental)
                .amount(new BigDecimal("2500.00"))
                .currency(CurrencyType.TRY)
                .status(PaymentStatus.AUTHORIZED)
                .transactionId("STUB-ABC123DEF456")
                .build();
    }

    @Nested
    @DisplayName("requestRental Tests")
    class RequestRentalTests {

        @Test
        @DisplayName("Should successfully create rental request")
        void shouldSuccessfullyCreateRentalRequest() {

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));
            
            PricingResult pricingResult = PricingResult.builder()
                    .basePrice(new BigDecimal("500.00"))
                    .rentalDays(5)
                    .appliedModifiers(Collections.emptyList())
                    .combinedMultiplier(BigDecimal.ONE)
                    .finalPrice(new BigDecimal("2500.00"))
                    .build();
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(pricingResult);

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.REQUESTED)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.requestRental(rentalRequest, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getStatus()).isEqualTo(RentalStatus.REQUESTED);

            verify(rentalRepository).save(any(Rental.class));
            verify(rentalRepository).countOverlappingRentals(eq(1L), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when car not found")
        void shouldThrowExceptionWhenCarNotFound() {
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when car is not available")
        void shouldThrowExceptionWhenCarIsNotAvailable() {
            testCar.setCarStatusType(CarStatusType.SOLD);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarNotAvailableException.class);
        }

        @Test
        @DisplayName("Should throw exception when dates overlap")
        void shouldThrowExceptionWhenDatesOverlap() {
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(1L);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(RentalDateOverlapException.class);
        }

        @Test
        @DisplayName("Should throw exception when start date is in past")
        void shouldThrowExceptionWhenStartDateIsInPast() {
            rentalRequest.setStartDate(LocalDate.now().minusDays(1));

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(RentalValidationException.class);
        }

        @Test
        @DisplayName("Should calculate days correctly")
        void shouldCalculateDaysCorrectly() {
            rentalRequest.setStartDate(LocalDate.now().plusDays(1));
            rentalRequest.setEndDate(LocalDate.now().plusDays(5));

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            
            PricingResult pricingResult = PricingResult.builder()
                    .basePrice(new BigDecimal("500.00"))
                    .rentalDays(5)
                    .appliedModifiers(Collections.emptyList())
                    .combinedMultiplier(BigDecimal.ONE)
                    .finalPrice(new BigDecimal("2500.00"))
                    .build();
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(pricingResult);
            
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
                Rental rental = invocation.getArgument(0);
                assertThat(rental.getDays()).isEqualTo(5);
                return rental;
            });

            RentalResponseDto responseDto = RentalResponseDto.builder().id(1L).build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            rentalService.requestRental(rentalRequest, "testuser");

            verify(rentalRepository).save(any(Rental.class));
        }
    }

    @Nested
    @DisplayName("confirmRental Tests")
    class ConfirmRentalTests {

        @Test
        @DisplayName("Should successfully confirm rental and authorize payment")
        void shouldSuccessfullyConfirmRentalAndAuthorizePayment() {

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentGateway.authorize(any(), any(), anyString()))
                    .thenReturn(PaymentResult.success("STUB-ABC123", "Authorized"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.CONFIRMED)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.confirmRental(1L);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RentalStatus.CONFIRMED);

            verify(paymentGateway).authorize(eq(new BigDecimal("2500.00")), eq(CurrencyType.TRY), anyString());
            verify(paymentRepository).save(any(Payment.class));
            verify(rentalRepository).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should throw exception when rental not found")
        void shouldThrowExceptionWhenRentalNotFound() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> rentalService.confirmRental(1L))
                    .isInstanceOf(RentalNotFoundException.class);
        }

        @Test
        @DisplayName("Should throw exception when rental is not in REQUESTED status")
        void shouldThrowExceptionWhenRentalIsNotInRequestedStatus() {
            testRental.setStatus(RentalStatus.CONFIRMED);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));

            assertThatThrownBy(() -> rentalService.confirmRental(1L))
                    .isInstanceOf(InvalidRentalStateException.class);
        }

        @Test
        @DisplayName("Should throw exception when payment authorization fails")
        void shouldThrowExceptionWhenPaymentAuthorizationFails() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentGateway.authorize(any(), any(), anyString()))
                    .thenReturn(PaymentResult.failure("Payment failed"));

            assertThatThrownBy(() -> rentalService.confirmRental(1L))
                    .isInstanceOf(PaymentFailedException.class);
        }
    }

    @Nested
    @DisplayName("pickupRental Tests")
    class PickupRentalTests {

        @Test
        @DisplayName("Should successfully process pickup and capture payment")
        void shouldSuccessfullyProcessPickupAndCapturePayment() {

            testRental.setStatus(RentalStatus.CONFIRMED);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.capture(anyString(), any()))
                    .thenReturn(PaymentResult.success("STUB-ABC123", "Captured"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.IN_USE)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.pickupRental(1L, "Pickup notes");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RentalStatus.IN_USE);

            verify(paymentGateway).capture(eq("STUB-ABC123DEF456"), eq(new BigDecimal("2500.00")));
            verify(paymentRepository).save(any(Payment.class));
        }

        @Test
        @DisplayName("Should throw exception when rental is not in CONFIRMED status")
        void shouldThrowExceptionWhenRentalIsNotInConfirmedStatus() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));

            assertThatThrownBy(() -> rentalService.pickupRental(1L, "Notes"))
                    .isInstanceOf(InvalidRentalStateException.class);
        }
    }

    @Nested
    @DisplayName("returnRental Tests")
    class ReturnRentalTests {

        @Test
        @DisplayName("Should successfully process return")
        void shouldSuccessfullyProcessReturn() {

            testRental.setStatus(RentalStatus.IN_USE);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.RETURNED)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.returnRental(1L, "Return notes");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RentalStatus.RETURNED);

            verify(rentalRepository).save(any(Rental.class));
        }
    }

    @Nested
    @DisplayName("cancelRental Tests")
    class CancelRentalTests {

        @Test
        @DisplayName("Should successfully cancel REQUESTED rental without refund")
        void shouldSuccessfullyCancelRequestedRentalWithoutRefund() {

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.CANCELLED)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.cancelRental(1L, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RentalStatus.CANCELLED);

            verify(paymentGateway, never()).refund(anyString(), any());
        }

        @Test
        @DisplayName("Should successfully cancel CONFIRMED rental with refund")
        void shouldSuccessfullyCancelConfirmedRentalWithRefund() {

            testRental.setStatus(RentalStatus.CONFIRMED);
            testCar.setCarStatusType(CarStatusType.RESERVED);
            testPayment.setStatus(PaymentStatus.CAPTURED);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any()))
                    .thenReturn(PaymentResult.success("STUB-REFUND123", "Refunded"));
            when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> invocation.getArgument(0));

            RentalResponseDto responseDto = RentalResponseDto.builder()
                    .id(1L)
                    .status(RentalStatus.CANCELLED)
                    .build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            RentalResponseDto result = rentalService.cancelRental(1L, "testuser");

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(RentalStatus.CANCELLED);

            verify(paymentGateway).refund(eq("STUB-ABC123DEF456"), eq(new BigDecimal("2500.00")));
            verify(rentalRepository).save(any(Rental.class));
        }

        @Test
        @DisplayName("Should throw exception when user tries to cancel other user's rental")
        void shouldThrowExceptionWhenUserTriesToCancelOtherUsersRental() {
            User otherUser = User.builder()
                    .id(2L)
                    .username("otheruser")
                    .roles(Set.of(Role.USER))
                    .build();

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("otheruser")).thenReturn(Optional.of(otherUser));

            assertThatThrownBy(() -> rentalService.cancelRental(1L, "otheruser"))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("getMyRentals Tests")
    class GetMyRentalsTests {

        @Test
        @DisplayName("Should return user's rentals")
        void shouldReturnUsersRentals() {

            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> rentalPage = new PageImpl<>(List.of(testRental), pageable, 1);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(rentalRepository.findByUserIdAndIsDeletedFalse(1L, pageable)).thenReturn(rentalPage);

            RentalResponseDto responseDto = RentalResponseDto.builder().id(1L).build();
            when(rentalMapper.toDto(testRental)).thenReturn(responseDto);

            Page<RentalResponseDto> result = rentalService.getMyRentals("testuser", pageable);

            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return empty page when user has no rentals")
        void shouldReturnEmptyPageWhenUserHasNoRentals() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<Rental> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(rentalRepository.findByUserIdAndIsDeletedFalse(1L, pageable)).thenReturn(emptyPage);

            Page<RentalResponseDto> result = rentalService.getMyRentals("testuser", pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should reject null start date")
        void shouldRejectNullStartDate() {
            rentalRequest.setStartDate(null);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should reject null end date")
        void shouldRejectNullEndDate() {
            rentalRequest.setEndDate(null);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should reject end date before start date")
        void shouldRejectEndDateBeforeStartDate() {
            rentalRequest.setStartDate(LocalDate.now().plusDays(5));
            rentalRequest.setEndDate(LocalDate.now().plusDays(1));

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(RentalValidationException.class);
        }

        @Test
        @DisplayName("Should accept same start and end date as 1 day rental")
        void shouldAcceptSameStartAndEndDateAsOneDayRental() {
            LocalDate sameDate = LocalDate.now().plusDays(1);
            rentalRequest.setStartDate(sameDate);
            rentalRequest.setEndDate(sameDate);

            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            
            PricingResult pricingResult = PricingResult.builder()
                    .basePrice(new BigDecimal("500.00"))
                    .rentalDays(1)
                    .appliedModifiers(Collections.emptyList())
                    .combinedMultiplier(BigDecimal.ONE)
                    .finalPrice(new BigDecimal("500.00"))
                    .build();
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(pricingResult);
            
            when(rentalRepository.save(any(Rental.class))).thenAnswer(invocation -> {
                Rental rental = invocation.getArgument(0);
                assertThat(rental.getDays()).isEqualTo(1);
                return rental;
            });

            RentalResponseDto responseDto = RentalResponseDto.builder().id(1L).build();
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(responseDto);

            rentalService.requestRental(rentalRequest, "testuser");

            verify(rentalRepository).save(any(Rental.class));
        }
    }

    @Nested
    @DisplayName("Payment Gateway Tests")
    class PaymentGatewayTests {

        @Test
        @DisplayName("Should handle payment authorization failure")
        void shouldHandlePaymentAuthorizationFailure() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentGateway.authorize(any(), any(), anyString()))
                    .thenReturn(PaymentResult.failure("Payment failed"));

            assertThatThrownBy(() -> rentalService.confirmRental(1L))
                    .isInstanceOf(PaymentFailedException.class);
        }

        @Test
        @DisplayName("Should handle payment capture failure")
        void shouldHandlePaymentCaptureFailure() {
            testRental.setStatus(RentalStatus.CONFIRMED);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.capture(anyString(), any()))
                    .thenReturn(PaymentResult.failure("Capture failed"));

            assertThatThrownBy(() -> rentalService.pickupRental(1L, "Notes"))
                    .isInstanceOf(PaymentFailedException.class);
        }

        @Test
        @DisplayName("Should handle payment refund failure")
        void shouldHandlePaymentRefundFailure() {
            testRental.setStatus(RentalStatus.CONFIRMED);
            testPayment.setStatus(PaymentStatus.CAPTURED);

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(userRepository.findByUsernameAndIsDeletedFalse("testuser")).thenReturn(Optional.of(testUser));
            when(paymentRepository.findByRentalIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testPayment));
            when(paymentGateway.refund(anyString(), any()))
                    .thenReturn(PaymentResult.failure("Refund failed"));

            assertThatThrownBy(() -> rentalService.cancelRental(1L, "testuser"))
                    .isInstanceOf(PaymentFailedException.class);
        }
    }
}


