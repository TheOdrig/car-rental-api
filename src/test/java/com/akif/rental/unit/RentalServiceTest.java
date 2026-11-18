package com.akif.rental.unit;

import com.akif.auth.api.UserLookup;
import com.akif.auth.api.UserRentalStatisticsDto;
import com.akif.auth.api.UserDto;
import com.akif.car.api.CarDto;
import com.akif.car.api.CarCatalog;
import com.akif.car.api.CarStatusType;
import com.akif.car.api.PricingResult;
import com.akif.car.api.CarDatesUnavailableException;
import com.akif.car.api.CarNotAvailableException;
import com.akif.car.api.DynamicPricingService;
import com.akif.payment.api.*;
import com.akif.payment.api.PaymentFailedException;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.api.RentalRequest;
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
import java.time.Clock;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import com.akif.payment.api.CreatePaymentRequest;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.akif.rental.internal.config.RentalProperties;
import com.akif.rental.internal.exception.OpenRentalRequestLimitException;
import com.akif.shared.enums.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RentalService Unit Tests")
class RentalServiceTest {

    @Mock
    private RentalRepository rentalRepository;

    @Mock
    private CarCatalog carCatalog;

    @Mock
    private UserLookup userLookup;

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

    @Spy
    private Clock clock = Clock.systemDefaultZone();

    @Spy
    private RentalProperties rentalProperties = new RentalProperties();

    @InjectMocks
    private RentalServiceImpl rentalService;

    private UserDto testUser;
    private CarDto testCar;
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
                null,
                null,
                Set.of(Role.USER),
                true);

        testCar = carWith(CarStatusType.AVAILABLE);

        rentalRequest = new RentalRequest(
                1L,
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(5),
                "Test rental request");

        testRental = Rental.builder()
                .id(1L)
                .userId(testUser.id())
                .userEmail(testUser.email())
                .userFullName(testUser.firstName() + " " + testUser.lastName())
                .carId(testCar.id())
                .carBrand(testCar.brand())
                .carModel(testCar.model())
                .carLicensePlate(testCar.licensePlate())
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
                null,
                null,
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
                LocalDateTime.now());
    }

    @Nested
    @DisplayName("Request Rental Tests")
    class RequestRentalTests {

        @Test
        @DisplayName("Should successfully create rental request")
        void shouldSuccessfullyCreateRentalRequest() {
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(testPricingResult);
            when(rentalRepository.saveAndFlush(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.requestRental(rentalRequest, "testuser");

            assertThat(result).isNotNull();
            verify(userLookup).getUserByUsername("testuser");
            verify(carCatalog).getCarForBooking(1L);
            verify(rentalRepository).saveAndFlush(any(Rental.class));
        }

        @Test
        @DisplayName("Should throw exception when car is not available")
        void shouldThrowExceptionWhenCarNotAvailable() {
            CarDto maintenanceCar = carWith(CarStatusType.MAINTENANCE);

            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(maintenanceCar);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarNotAvailableException.class);

            verify(rentalRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should throw exception when date overlap exists")
        void shouldThrowExceptionWhenDateOverlapExists() {
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(1L);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarDatesUnavailableException.class)
                    .satisfies(error -> assertThat(((CarDatesUnavailableException) error).getHttpStatus())
                            .isEqualTo(HttpStatus.CONFLICT));

            verify(rentalRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Should map the database exclusion violation to a conflict")
        void shouldMapExclusionViolationToConflict() {
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(testPricingResult);
            when(rentalRepository.saveAndFlush(any(Rental.class))).thenThrow(
                    new DataIntegrityViolationException("exclusion", new SQLException("conflict", "23P01")));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(CarDatesUnavailableException.class)
                    .satisfies(error -> {
                        CarDatesUnavailableException exception = (CarDatesUnavailableException) error;
                        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(exception.getErrorCode()).isEqualTo("CAR_DATES_UNAVAILABLE");
                    });
        }

        @Test
        @DisplayName("Should rethrow other integrity violations")
        void shouldRethrowOtherIntegrityViolations() {
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(testPricingResult);
            when(rentalRepository.saveAndFlush(any(Rental.class))).thenThrow(
                    new DataIntegrityViolationException("unique", new SQLException("duplicate", "23505")));

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("Should throw exception when start date is in past")
        void shouldThrowExceptionWhenStartDateIsInPast() {
            RentalRequest pastRequest = new RentalRequest(
                    1L,
                    LocalDate.now().minusDays(1),
                    LocalDate.now().plusDays(5),
                    null);

            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);

            assertThatThrownBy(() -> rentalService.requestRental(pastRequest, "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("past");
        }
    }

    @Nested
    @DisplayName("Request limits")
    class RequestLimitTests {

        private void prepare() {
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
        }

        @Test
        @DisplayName("rejects an end date equal to the start date")
        void rejectsSameDayRange() {
            prepare();
            LocalDate day = LocalDate.now().plusDays(3);

            assertThatThrownBy(() -> rentalService.requestRental(new RentalRequest(1L, day, day, null), "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("End date must be after start date");
        }

        @Test
        @DisplayName("rejects a rental longer than the configured maximum")
        void rejectsTooLongRental() {
            prepare();
            LocalDate start = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> rentalService.requestRental(
                    new RentalRequest(1L, start, start.plusDays(30), null), "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("30 days");
        }

        @Test
        @DisplayName("accepts a rental of exactly the maximum inclusive length")
        void acceptsMaximumLength() {
            prepare();
            LocalDate start = LocalDate.now().plusDays(1);
            when(rentalRepository.countOverlappingRentals(anyLong(), any(), any())).thenReturn(0L);
            when(dynamicPricingService.calculatePrice(anyLong(), any(), any(), any())).thenReturn(testPricingResult);
            when(rentalRepository.saveAndFlush(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            assertThat(rentalService.requestRental(
                    new RentalRequest(1L, start, start.plusDays(29), null), "testuser")).isNotNull();
        }

        @Test
        @DisplayName("rejects a start date beyond the configured lead time")
        void rejectsTooFarAhead() {
            prepare();
            LocalDate start = LocalDate.now().plusDays(181);

            assertThatThrownBy(() -> rentalService.requestRental(
                    new RentalRequest(1L, start, start.plusDays(2), null), "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("180 days");
        }

        @Test
        @DisplayName("honours a tighter configured maximum length")
        void honoursConfiguredLength() {
            prepare();
            rentalProperties.setMaxRentalDays(7);
            LocalDate start = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> rentalService.requestRental(
                    new RentalRequest(1L, start, start.plusDays(7), null), "testuser"))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("7 days");
        }

        @Test
        @DisplayName("rejects a new request once the user has the maximum number of open requests")
        void rejectsWhenOpenRequestCapReached() {
            prepare();
            when(rentalRepository.countByUserIdAndStatusAndIsDeletedFalse(1L, RentalStatus.REQUESTED))
                    .thenReturn(3);

            assertThatThrownBy(() -> rentalService.requestRental(rentalRequest, "testuser"))
                    .isInstanceOf(OpenRentalRequestLimitException.class)
                    .satisfies(error -> {
                        OpenRentalRequestLimitException exception = (OpenRentalRequestLimitException) error;
                        assertThat(exception.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(exception.getErrorCode()).isEqualTo("OPEN_RENTAL_REQUEST_LIMIT_EXCEEDED");
                    });
            verify(rentalRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("Fleet status guards on confirm and pickup")
    class FleetGuardTests {

        private CarDto carWithStatus(CarStatusType status) {
            return carWith(status);
        }

        @Test
        @DisplayName("confirm is rejected when the car is no longer AVAILABLE")
        void confirmRejectedWhenCarUnavailable() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(carWithStatus(CarStatusType.MAINTENANCE));

            assertThatThrownBy(() -> rentalService.confirmRental(1L, null))
                    .isInstanceOf(CarNotAvailableException.class);

            verify(paymentService, never()).createPayment(any());
        }

        @Test
        @DisplayName("pickup is rejected when the car is no longer AVAILABLE")
        void pickupRejectedWhenCarUnavailable() {
            testRental.confirm(null);
            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(carWithStatus(CarStatusType.DAMAGED));

            assertThatThrownBy(() -> rentalService.pickupRental(1L, null))
                    .isInstanceOf(CarNotAvailableException.class);

            verify(paymentService, never()).capturePayment(anyLong());
        }

        @Test
        @DisplayName("pickup is rejected with a conflict while another rental of the car is in use")
        void pickupRejectedWhenAnotherRentalInUse() {
            testRental.confirm(null);
            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(carWithStatus(CarStatusType.AVAILABLE));
            when(rentalRepository.existsByCarIdAndStatusAndIsDeletedFalseAndIdNot(
                    1L, RentalStatus.IN_USE, 1L)).thenReturn(true);

            assertThatThrownBy(() -> rentalService.pickupRental(1L, null))
                    .isInstanceOf(CarDatesUnavailableException.class)
                    .satisfies(error -> assertThat(((CarDatesUnavailableException) error).getHttpStatus())
                            .isEqualTo(HttpStatus.CONFLICT));

            verify(paymentService, never()).capturePayment(anyLong());
            assertThat(testRental.getStatus()).isEqualTo(RentalStatus.CONFIRMED);
        }
    }

    @Nested
    @DisplayName("Confirm Rental Tests")
    class ConfirmRentalTests {

        @Test
        @DisplayName("Should confirm the rental and create a pending payment without charging")
        void shouldConfirmRentalAndCreatePendingPayment() {
            PaymentDto paymentDto = createTestPaymentDto();

            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(paymentService.createPayment(any())).thenReturn(paymentDto);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.confirmRental(1L, null);

            assertThat(result).isNotNull();
            ArgumentCaptor<CreatePaymentRequest> request = ArgumentCaptor.forClass(CreatePaymentRequest.class);
            verify(paymentService).createPayment(request.capture());
            assertThat(request.getValue().amount()).isEqualByComparingTo(testRental.getTotalPrice());
            assertThat(request.getValue().currency()).isEqualTo(testRental.getCurrency());
            verifyNoMoreInteractions(paymentService);
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should keep the rental REQUESTED when the car is unavailable and not create a payment")
        void shouldNotCreatePaymentWhenCarUnavailable() {
            when(rentalRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(carWith(CarStatusType.MAINTENANCE));

            assertThatThrownBy(() -> rentalService.confirmRental(1L, null))
                    .isInstanceOf(CarNotAvailableException.class);

            verifyNoInteractions(paymentService);
            assertThat(testRental.getStatus()).isEqualTo(RentalStatus.REQUESTED);
        }
    }

    @Nested
    @DisplayName("Cancel Rental Tests")
    class CancelRentalTests {

        @Test
        @DisplayName("Should successfully cancel requested rental without refund")
        void shouldSuccessfullyCancelRequestedRentalWithoutRefund() {
            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.cancelRental(1L, "testuser", null);

            assertThat(result).isNotNull();
            verify(paymentService, never()).refundPayment(anyLong(), any(), any());
            verify(eventPublisher).publishEvent(any(Object.class));
        }

        @Test
        @DisplayName("Should throw exception when user tries to cancel other user's rental")
        void shouldThrowExceptionWhenUserTriesToCancelOtherUsersRental() {
            UserDto otherUser = new UserDto(2L, "otheruser", "other@example.com", "Other", "User", null, null,
                    Set.of(Role.USER),
                    true);

            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(userLookup.getUserByUsername("otheruser")).thenReturn(otherUser);

            assertThatThrownBy(() -> rentalService.cancelRental(1L, "otheruser", null))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("Pickup and Return Tests")
    class PickupAndReturnTests {

        @Test
        @DisplayName("Should successfully process pickup and capture payment")
        void shouldSuccessfullyProcessPickupAndCapturePayment() {
            testRental.confirm(null);
            PaymentDto paymentDto = createTestPaymentDto();
            PaymentResult captureResult = new PaymentResult(true, "TXN-123", "Captured");

            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(carCatalog.getCarForBooking(1L)).thenReturn(testCar);
            when(paymentService.getPaymentByRentalId(1L)).thenReturn(Optional.of(paymentDto));
            when(paymentService.capturePayment(anyLong())).thenReturn(captureResult);
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.pickupRental(1L, "Pickup notes");

            assertThat(result).isNotNull();
            verify(paymentService).capturePayment(paymentDto.id());
        }

        @Test
        @DisplayName("Should successfully process return")
        void shouldSuccessfullyProcessReturn() {
            testRental.confirm(null);
            testRental.pickUp(null);

            when(rentalRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(testRental));
            when(rentalRepository.save(any(Rental.class))).thenReturn(testRental);
            when(rentalMapper.toDto(any(Rental.class))).thenReturn(testRentalResponse);

            RentalResponse result = rentalService.returnRental(1L, "Return notes");

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("User statistics")
    class UserStatisticsTests {

        @Test
        @DisplayName("spending is reported per currency and never summed across currencies")
        void spendingIsGroupedByCurrency() {
            when(rentalRepository.sumTotalPriceByCurrencyForReturnedRentals(1L)).thenReturn(List.of(
                    new Object[] { CurrencyType.EUR, 1L, new BigDecimal("100.00") },
                    new Object[] { CurrencyType.TRY, 2L, new BigDecimal("900.00") }));

            UserRentalStatisticsDto stats = rentalService.getUserStatistics(1L);

            assertThat(stats.spentByCurrency()).containsExactly(
                    new UserRentalStatisticsDto.CurrencyTotals(CurrencyType.EUR, 1L, new BigDecimal("100.00")),
                    new UserRentalStatisticsDto.CurrencyTotals(CurrencyType.TRY, 2L, new BigDecimal("900.00")));
        }

        @Test
        @DisplayName("a user with no returned rentals has no spending entries")
        void noSpendingMeansNoEntries() {
            when(rentalRepository.sumTotalPriceByCurrencyForReturnedRentals(1L)).thenReturn(List.of());

            assertThat(rentalService.getUserStatistics(1L).spentByCurrency()).isEmpty();
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

            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
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

            when(userLookup.getUserByUsername("testuser")).thenReturn(testUser);
            when(rentalRepository.findByUserIdAndIsDeletedFalse(testUser.id(), pageable)).thenReturn(emptyPage);

            Page<RentalResponse> result = rentalService.getMyRentals("testuser", pageable);

            assertThat(result).isEmpty();
        }
    }

    private static CarDto carWith(CarStatusType status) {
        return new CarDto(1L, "Toyota", "Corolla", "34ABC123", new BigDecimal("500.00"), CurrencyType.TRY, status,
                null, null, null, status == CarStatusType.AVAILABLE, false);
    }

    private PaymentDto createTestPaymentDto() {
        return new PaymentDto(
                1L,
                testRental.getId(),
                testUser.email(),
                testCar.licensePlate(),
                testRental.getTotalPrice(),
                testRental.getCurrency(),
                PaymentStatus.AUTHORIZED,
                "CARD",
                "TXN-123",
                null,
                null,
                null);
    }
}
