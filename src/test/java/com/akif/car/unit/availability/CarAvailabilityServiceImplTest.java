package com.akif.car.unit.availability;

import com.akif.car.api.DayAvailabilityDto;
import com.akif.car.domain.Car;
import com.akif.car.api.AvailabilityStatus;
import com.akif.car.api.CarStatusType;
import com.akif.car.internal.dto.availability.*;
import com.akif.car.api.PriceModifier;
import com.akif.car.api.PricingResult;
import com.akif.car.internal.exception.CarNotFoundException;
import com.akif.car.internal.repository.CarRepository;
import com.akif.car.internal.service.availability.impl.CarAvailabilityServiceImpl;
import com.akif.car.internal.service.pricing.BatchPricing;
import com.akif.currency.api.ConversionResult;
import com.akif.currency.api.CurrencyConversionService;
import com.akif.currency.api.RateSource;
import com.akif.car.internal.exception.AvailabilityValidationException;
import com.akif.car.api.BookedPeriod;
import com.akif.car.api.CarBookingCalendar;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.time.Clock;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarAvailabilityServiceImpl Unit Tests")
class CarAvailabilityServiceImplTest {

        @Mock
        private CarRepository carRepository;

        @Mock
        private CarBookingCalendar bookingCalendar;

        @Mock
        private BatchPricing batchPricing;

        @Mock
        private CurrencyConversionService currencyConversionService;

        @Mock
        private com.akif.car.internal.config.AvailabilityProperties availabilityProperties;

    @Spy
    private Clock clock = Clock.systemDefaultZone();

        @InjectMocks
        private CarAvailabilityServiceImpl carAvailabilityService;

        private Car testCar;
        private AvailabilitySearchRequest searchRequest;
        private PricingResult pricingResult;

        @BeforeEach
        void setUp() {
                org.mockito.Mockito.lenient().when(availabilityProperties.maxSearchDays()).thenReturn(90);
                testCar = Car.builder()
                                .id(1L)
                                .licensePlate("34ABC123")
                                .brand("Toyota")
                                .model("Corolla")
                                .productionYear(2020)
                                .bodyType("Sedan")
                                .fuelType("Gasoline")
                                .transmissionType("Automatic")
                                .seats(5)
                                .price(new BigDecimal("500"))
                                .currencyType(CurrencyType.TRY)
                                .carStatusType(CarStatusType.AVAILABLE)
                                .imageUrl("https://example.com/car.jpg")
                                .rating(new BigDecimal("4.5"))
                                .isDeleted(false)
                                .build();

                searchRequest = AvailabilitySearchRequest.builder()
                                .startDate(LocalDate.now().plusDays(1))
                                .endDate(LocalDate.now().plusDays(5))
                                .page(0)
                                .size(20)
                                .sortBy("price")
                                .sortDirection("asc")
                                .build();

                pricingResult = PricingResult.builder()
                                .basePrice(new BigDecimal("500"))
                                .rentalDays(5)
                                .appliedModifiers(List.of(PriceModifier.discount("Early booking", new BigDecimal("0.9"),
                                                "Early booking discount")))
                                .combinedMultiplier(new BigDecimal("0.9"))
                                .finalPrice(new BigDecimal("2250"))
                                .build();
        }

        @Nested
        @DisplayName("Search Available Cars Tests")
        class SearchAvailableCarsTests {

                @Test
                @DisplayName("Should return available cars when valid date range provided")
                void shouldReturnAvailableCarsWhenValidDateRangeProvided() {
                        Page<Car> carPage = new PageImpl<>(List.of(testCar));
                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result).isNotNull();
                        assertThat(result.cars()).hasSize(1);
                        assertThat(result.totalElements()).isEqualTo(1);
                        assertThat(result.rentalDays()).isEqualTo(5);
                        assertThat(result.searchStartDate()).isEqualTo(searchRequest.startDate());
                        assertThat(result.searchEndDate()).isEqualTo(searchRequest.endDate());

                        verify(carRepository).findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
                        verify(batchPricing).priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class));
                }

                @Test
                @DisplayName("Should apply all filters when multiple criteria provided")
                void shouldApplyAllFiltersWhenMultipleCriteriaProvided() {
                        searchRequest = searchRequest.toBuilder().brand("Toyota").build();
                        searchRequest = searchRequest.toBuilder().model("Corolla").build();
                        searchRequest = searchRequest.toBuilder().bodyType("Sedan").build();
                        searchRequest = searchRequest.toBuilder().minPrice(new BigDecimal("400")).build();
                        searchRequest = searchRequest.toBuilder().maxPrice(new BigDecimal("600")).build();

                        Page<Car> carPage = new PageImpl<>(List.of(testCar));
                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        anyString(), anyString(), any(), any(), anyString(), any(),
                                        any(BigDecimal.class), any(BigDecimal.class), any(), any(),
                                        any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result).isNotNull();
                        assertThat(result.cars()).hasSize(1);

                        verify(carRepository).findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        anyString(), anyString(), any(), any(), anyString(), any(),
                                        any(BigDecimal.class), any(BigDecimal.class), any(), any(),
                                        any(Pageable.class));
                }

                @Test
                @DisplayName("Should apply currency conversion when target currency specified")
                void shouldApplyCurrencyConversionWhenTargetCurrencySpecified() {
                        searchRequest = searchRequest.toBuilder().targetCurrency(CurrencyType.USD).build();
                        Page<Car> carPage = new PageImpl<>(List.of(testCar));

                        ConversionResult dailyConversion = new ConversionResult(
                                        new BigDecimal("450"), CurrencyType.TRY,
                                        new BigDecimal("15"), CurrencyType.USD,
                                        new BigDecimal("0.033"), Instant.now(), RateSource.LIVE);
                        ConversionResult totalConversion = new ConversionResult(
                                        new BigDecimal("2250"), CurrencyType.TRY,
                                        new BigDecimal("75"), CurrencyType.USD,
                                        new BigDecimal("0.033"), Instant.now(), RateSource.LIVE);

                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));
                        when(currencyConversionService.convert(any(BigDecimal.class),
                                        eq(CurrencyType.TRY), eq(CurrencyType.USD)))
                                        .thenReturn(dailyConversion, totalConversion);

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result).isNotNull();
                        assertThat(result.cars()).hasSize(1);
                        assertThat(result.cars().get(0).currency()).isEqualTo(CurrencyType.USD);
                        assertThat(result.cars().get(0).dailyRate()).isEqualTo(new BigDecimal("15"));
                        assertThat(result.cars().get(0).totalPrice()).isEqualTo(new BigDecimal("75"));

                        verify(currencyConversionService, times(2)).convert(
                                        any(BigDecimal.class), eq(CurrencyType.TRY), eq(CurrencyType.USD));
                }

                @Test
                @DisplayName("Should respect pagination parameters")
                void shouldRespectPaginationParameters() {
                        searchRequest = searchRequest.toBuilder().page(1).build();
                        searchRequest = searchRequest.toBuilder().size(10).build();
                        Page<Car> carPage = new PageImpl<>(List.of(testCar),
                                        org.springframework.data.domain.PageRequest.of(1, 10), 25);

                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result.currentPage()).isEqualTo(1);
                        assertThat(result.pageSize()).isEqualTo(10);
                        assertThat(result.totalPages()).isEqualTo(3);
                        assertThat(result.totalElements()).isEqualTo(25);
                }
        }

        @Nested
        @DisplayName("Date Validation Tests")
        class DateValidationTests {

                @Test
                @DisplayName("Should throw exception when start date is in the past")
                void shouldThrowExceptionWhenStartDateIsInThePast() {
                        searchRequest = searchRequest.toBuilder().startDate(LocalDate.now().minusDays(1)).build();

                        assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Start date cannot be in the past");

                        verify(carRepository, never()).findAvailableCarsForDateRange(
                                        any(), any(), any(), any(), any(), any(), any(), any(), any(),
                                        any(), any(), any(), any());
                }

                @Test
                @DisplayName("Should throw exception when end date is before start date")
                void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
                        searchRequest = searchRequest.toBuilder().startDate(LocalDate.now().plusDays(5)).build();
                        searchRequest = searchRequest.toBuilder().endDate(LocalDate.now().plusDays(1)).build();

                        assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("End date must be after start date");
                }

                @Test
                @DisplayName("Should throw exception when rental period exceeds 90 days")
                void shouldThrowExceptionWhenRentalPeriodExceeds90Days() {
                        searchRequest = searchRequest.toBuilder().startDate(LocalDate.now().plusDays(1)).build();
                        searchRequest = searchRequest.toBuilder().endDate(LocalDate.now().plusDays(92)).build();

                        assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Rental period cannot exceed 90 days");
                }

                @Test
                @DisplayName("Should throw exception when page size is less than 1")
                void shouldThrowExceptionWhenPageSizeIsLessThan1() {
                        searchRequest = searchRequest.toBuilder().size(0).build();

                        assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Page size must be between 1 and 100");
                }

                @Test
                @DisplayName("Should throw exception when page size exceeds 100")
                void shouldThrowExceptionWhenPageSizeExceeds100() {
                        searchRequest = searchRequest.toBuilder().size(101).build();

                        assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Page size must be between 1 and 100");
                }
        }

        @Nested
        @DisplayName("Is Car Available Tests")
        class IsCarAvailableTests {

                @Test
                @DisplayName("Should return true when car is available")
                void shouldReturnTrueWhenCarIsAvailable() {
                        LocalDate startDate = LocalDate.now().plusDays(1);
                        LocalDate endDate = LocalDate.now().plusDays(5);

                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
                        when(bookingCalendar.countActiveBookings(1L, startDate, endDate)).thenReturn(0L);

                        boolean result = carAvailabilityService.isCarAvailable(1L, startDate, endDate);

                        assertThat(result).isTrue();
                        verify(carRepository).findByIdAndIsDeletedFalse(1L);
                        verify(bookingCalendar).countActiveBookings(1L, startDate, endDate);
                }

                @Test
                @DisplayName("Should return false when car has blocking status")
                void shouldReturnFalseWhenCarHasBlockingStatus() {
                        testCar.setCarStatusType(CarStatusType.MAINTENANCE);
                        LocalDate startDate = LocalDate.now().plusDays(1);
                        LocalDate endDate = LocalDate.now().plusDays(5);

                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));

                        boolean result = carAvailabilityService.isCarAvailable(1L, startDate, endDate);

                        assertThat(result).isFalse();
                        verify(carRepository).findByIdAndIsDeletedFalse(1L);
                        verify(bookingCalendar, never()).countActiveBookings(any(), any(), any());
                }

                @Test
                @DisplayName("Should return false when car has overlapping rentals")
                void shouldReturnFalseWhenCarHasOverlappingRentals() {
                        LocalDate startDate = LocalDate.now().plusDays(1);
                        LocalDate endDate = LocalDate.now().plusDays(5);

                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
                        when(bookingCalendar.countActiveBookings(1L, startDate, endDate)).thenReturn(2L);

                        boolean result = carAvailabilityService.isCarAvailable(1L, startDate, endDate);

                        assertThat(result).isFalse();
                        verify(bookingCalendar).countActiveBookings(1L, startDate, endDate);
                }

                @Test
                @DisplayName("Should throw exception when car not found")
                void shouldThrowExceptionWhenCarNotFound() {
                        LocalDate startDate = LocalDate.now().plusDays(1);
                        LocalDate endDate = LocalDate.now().plusDays(5);

                        when(carRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

                        assertThatThrownBy(() -> carAvailabilityService.isCarAvailable(999L, startDate, endDate))
                                        .isInstanceOf(CarNotFoundException.class);

                        verify(carRepository).findByIdAndIsDeletedFalse(999L);
                }
        }

        @Nested
        @DisplayName("Calendar Generation Tests")
        class CalendarGenerationTests {

                @Test
                @DisplayName("Should generate calendar with all days in month")
                void shouldGenerateCalendarWithAllDaysInMonth() {
                        YearMonth month = YearMonth.now().plusMonths(1);
                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
                        when(bookingCalendar.findBookedPeriods(
                                        anyLong(), any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(List.of());

                        CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L,
                                        month);

                        assertThat(result).isNotNull();
                        assertThat(result.carId()).isEqualTo(1L);
                        assertThat(result.month()).isEqualTo(month);
                        assertThat(result.days()).hasSize(month.lengthOfMonth());
                        assertThat(result.carBlocked()).isFalse();
                        assertThat(result.blockReason()).isNull();

                        verify(carRepository).findByIdAndIsDeletedFalse(1L);
                        verify(bookingCalendar).findBookedPeriods(
                                        anyLong(), any(LocalDate.class), any(LocalDate.class));
                }

                @Test
                @DisplayName("Should mark days as unavailable when rentals exist")
                void shouldMarkDaysAsUnavailableWhenRentalsExist() {
                        YearMonth month = YearMonth.now().plusMonths(1);
                        LocalDate rentalStart = month.atDay(10);
                        LocalDate rentalEnd = month.atDay(15);

                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
                        when(bookingCalendar.findBookedPeriods(
                                        anyLong(), any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(List.of(new BookedPeriod(1L, rentalStart, rentalEnd)));

                        CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L,
                                        month);

                        assertThat(result.days()).hasSize(month.lengthOfMonth());

                        long unavailableDays = result.days().stream()
                                        .filter(day -> day.status() == AvailabilityStatus.UNAVAILABLE)
                                        .count();
                        assertThat(unavailableDays).isEqualTo(6);

                        DayAvailabilityDto unavailableDay = result.days().stream()
                                        .filter(day -> day.date().equals(rentalStart))
                                        .findFirst()
                                        .orElseThrow();
                        assertThat(unavailableDay.status()).isEqualTo(AvailabilityStatus.UNAVAILABLE);
                        assertThat(DayAvailabilityDto.class.getRecordComponents())
                                        .extracting(java.lang.reflect.RecordComponent::getName)
                                        .containsExactly("date", "status");
                }

                @Test
                @DisplayName("Should mark all days unavailable when car has blocking status")
                void shouldMarkAllDaysUnavailableWhenCarHasBlockingStatus() {
                        testCar.setCarStatusType(CarStatusType.DAMAGED);
                        YearMonth month = YearMonth.now().plusMonths(1);

                        when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
                        when(bookingCalendar.findBookedPeriods(
                                        anyLong(), any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(List.of());

                        CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L,
                                        month);

                        assertThat(result.carBlocked()).isTrue();
                        assertThat(result.blockReason()).isEqualTo(CarStatusType.DAMAGED.name());
                        assertThat(result.days()).allMatch(day -> day.status() == AvailabilityStatus.UNAVAILABLE);
                }

                @Test
                @DisplayName("Should throw exception when calendar month is in the past")
                void shouldThrowExceptionWhenCalendarMonthIsInThePast() {
                        YearMonth pastMonth = YearMonth.now().minusMonths(1);

                        assertThatThrownBy(() -> carAvailabilityService.getCarAvailabilityCalendar(1L, pastMonth))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Calendar month cannot be in the past");

                        verify(carRepository, never()).findByIdAndIsDeletedFalse(any());
                }

                @Test
                @DisplayName("Should throw exception when calendar month exceeds 3 months")
                void shouldThrowExceptionWhenCalendarMonthExceeds3Months() {
                        YearMonth futureMonth = YearMonth.now().plusMonths(4);

                        assertThatThrownBy(() -> carAvailabilityService.getCarAvailabilityCalendar(1L, futureMonth))
                                        .isInstanceOf(AvailabilityValidationException.class)
                                        .hasMessageContaining("Calendar only available up to 3 months in advance");

                        verify(carRepository, never()).findByIdAndIsDeletedFalse(any());
                }
        }

        @Nested
        @DisplayName("Pricing Integration Tests")
        class PricingIntegrationTests {

                @Test
                @DisplayName("Should include pricing information in search results")
                void shouldIncludePricingInformationInSearchResults() {
                        Page<Car> carPage = new PageImpl<>(List.of(testCar));
                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result.cars()).hasSize(1);
                        AvailableCarDto carDto = result.cars().get(0);
                        assertThat(carDto.dailyRate()).isNotNull();
                        assertThat(carDto.totalPrice()).isNotNull();
                        assertThat(carDto.currency()).isNotNull();
                        assertThat(carDto.appliedDiscounts()).isNotEmpty();
                        assertThat(carDto.appliedDiscounts()).contains("Early booking discount");

                        verify(batchPricing).priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class));
                }

                @Test
                @DisplayName("Should use dynamic pricing service for price calculation")
                void shouldUseDynamicPricingServiceForPriceCalculation() {
                        Page<Car> carPage = new PageImpl<>(List.of(testCar));
                        when(carRepository.findAvailableCarsForDateRange(
                                        anyCollection(), anyList(),
                                        isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                                        .thenReturn(carPage);
                        when(batchPricing.priceCars(anyCollection(), any(LocalDate.class),
                                        any(LocalDate.class), any(LocalDate.class)))
                                        .thenReturn(java.util.Map.of(1L, pricingResult));

                        AvailabilitySearchResponse result = carAvailabilityService.searchAvailableCars(searchRequest);

                        assertThat(result.cars().get(0).dailyRate()).isEqualTo(pricingResult.effectiveDailyPrice());
                        assertThat(result.cars().get(0).totalPrice()).isEqualTo(pricingResult.finalPrice());

                        verify(batchPricing).priceCars(
                                        anyCollection(),
                                        eq(searchRequest.startDate()),
                                        eq(searchRequest.endDate()),
                                        any(LocalDate.class));
                }
        }
}
