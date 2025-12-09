package com.akif.service.availability;

import com.akif.dto.availability.*;
import com.akif.shared.enums.AvailabilityStatus;
import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.RentalStatus;
import com.akif.exception.CarNotFoundException;
import com.akif.exception.RentalValidationException;
import com.akif.model.Car;
import com.akif.model.Rental;
import com.akif.model.User;
import com.akif.repository.CarRepository;
import com.akif.repository.RentalRepository;
import com.akif.service.availability.impl.CarAvailabilityServiceImpl;
import com.akif.service.currency.ICurrencyConversionService;
import com.akif.service.pricing.IDynamicPricingService;
import com.akif.service.pricing.PriceModifier;
import com.akif.service.pricing.PricingResult;
import com.akif.dto.currency.ConversionResult;
import com.akif.shared.enums.RateSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private RentalRepository rentalRepository;

    @Mock
    private IDynamicPricingService dynamicPricingService;

    @Mock
    private ICurrencyConversionService currencyConversionService;

    @InjectMocks
    private CarAvailabilityServiceImpl carAvailabilityService;

    private Car testCar;
    private AvailabilitySearchRequestDto searchRequest;
    private PricingResult pricingResult;

    @BeforeEach
    void setUp() {
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

        searchRequest = AvailabilitySearchRequestDto.builder()
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
                .appliedModifiers(List.of(PriceModifier.discount("Early booking", new BigDecimal("0.9"), "Early booking discount")))
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
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getCars()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getRentalDays()).isEqualTo(5);
            assertThat(result.getSearchStartDate()).isEqualTo(searchRequest.getStartDate());
            assertThat(result.getSearchEndDate()).isEqualTo(searchRequest.getEndDate());

            verify(carRepository).findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
            verify(dynamicPricingService).calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Should apply all filters when multiple criteria provided")
        void shouldApplyAllFiltersWhenMultipleCriteriaProvided() {
            searchRequest.setBrand("Toyota");
            searchRequest.setModel("Corolla");
            searchRequest.setBodyType("Sedan");
            searchRequest.setMinPrice(new BigDecimal("400"));
            searchRequest.setMaxPrice(new BigDecimal("600"));

            Page<Car> carPage = new PageImpl<>(List.of(testCar));
            when(carRepository.findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    eq("Toyota"), eq("Corolla"), isNull(), isNull(), eq("Sedan"), isNull(),
                    eq(new BigDecimal("400")), eq(new BigDecimal("600")), isNull(), isNull(), 
                    any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getCars()).hasSize(1);

            verify(carRepository).findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    eq("Toyota"), eq("Corolla"), isNull(), isNull(), eq("Sedan"), isNull(),
                    eq(new BigDecimal("400")), eq(new BigDecimal("600")), isNull(), isNull(), 
                    any(Pageable.class));
        }

        @Test
        @DisplayName("Should apply currency conversion when target currency specified")
        void shouldApplyCurrencyConversionWhenTargetCurrencySpecified() {
            searchRequest.setTargetCurrency(CurrencyType.USD);
            Page<Car> carPage = new PageImpl<>(List.of(testCar));
            
            ConversionResult dailyConversion = new ConversionResult(
                    new BigDecimal("450"), CurrencyType.TRY,
                    new BigDecimal("15"), CurrencyType.USD,
                    new BigDecimal("0.033"), LocalDateTime.now(), RateSource.LIVE);
            ConversionResult totalConversion = new ConversionResult(
                    new BigDecimal("2250"), CurrencyType.TRY,
                    new BigDecimal("75"), CurrencyType.USD,
                    new BigDecimal("0.033"), LocalDateTime.now(), RateSource.LIVE);

            when(carRepository.findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);
            when(currencyConversionService.convert(any(BigDecimal.class), 
                    eq(CurrencyType.TRY), eq(CurrencyType.USD)))
                    .thenReturn(dailyConversion, totalConversion);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result).isNotNull();
            assertThat(result.getCars()).hasSize(1);
            assertThat(result.getCars().get(0).getCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(result.getCars().get(0).getDailyRate()).isEqualTo(new BigDecimal("15"));
            assertThat(result.getCars().get(0).getTotalPrice()).isEqualTo(new BigDecimal("75"));

            verify(currencyConversionService, times(2)).convert(
                    any(BigDecimal.class), eq(CurrencyType.TRY), eq(CurrencyType.USD));
        }

        @Test
        @DisplayName("Should respect pagination parameters")
        void shouldRespectPaginationParameters() {
            searchRequest.setPage(1);
            searchRequest.setSize(10);
            Page<Car> carPage = new PageImpl<>(List.of(testCar), 
                    org.springframework.data.domain.PageRequest.of(1, 10), 25);

            when(carRepository.findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result.getCurrentPage()).isEqualTo(1);
            assertThat(result.getPageSize()).isEqualTo(10);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.getTotalElements()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    class DateValidationTests {

        @Test
        @DisplayName("Should throw exception when start date is in the past")
        void shouldThrowExceptionWhenStartDateIsInThePast() {
            searchRequest.setStartDate(LocalDate.now().minusDays(1));

            assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("Start date cannot be in the past");

            verify(carRepository, never()).findAvailableCarsForDateRange(
                    any(), any(), any(), any(), any(), any(), any(), any(), any(), 
                    any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Should throw exception when end date is before start date")
        void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
            searchRequest.setStartDate(LocalDate.now().plusDays(5));
            searchRequest.setEndDate(LocalDate.now().plusDays(1));

            assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("End date must be after start date");
        }

        @Test
        @DisplayName("Should throw exception when rental period exceeds 90 days")
        void shouldThrowExceptionWhenRentalPeriodExceeds90Days() {
            searchRequest.setStartDate(LocalDate.now().plusDays(1));
            searchRequest.setEndDate(LocalDate.now().plusDays(92));

            assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("Rental period cannot exceed 90 days");
        }

        @Test
        @DisplayName("Should throw exception when page size is less than 1")
        void shouldThrowExceptionWhenPageSizeIsLessThan1() {
            searchRequest.setSize(0);

            assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("Page size must be between 1 and 100");
        }

        @Test
        @DisplayName("Should throw exception when page size exceeds 100")
        void shouldThrowExceptionWhenPageSizeExceeds100() {
            searchRequest.setSize(101);

            assertThatThrownBy(() -> carAvailabilityService.searchAvailableCars(searchRequest))
                    .isInstanceOf(RentalValidationException.class)
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
            when(rentalRepository.countOverlappingRentals(1L, startDate, endDate)).thenReturn(0L);

            boolean result = carAvailabilityService.isCarAvailable(1L, startDate, endDate);

            assertThat(result).isTrue();
            verify(carRepository).findByIdAndIsDeletedFalse(1L);
            verify(rentalRepository).countOverlappingRentals(1L, startDate, endDate);
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
            verify(rentalRepository, never()).countOverlappingRentals(any(), any(), any());
        }

        @Test
        @DisplayName("Should return false when car has overlapping rentals")
        void shouldReturnFalseWhenCarHasOverlappingRentals() {
            LocalDate startDate = LocalDate.now().plusDays(1);
            LocalDate endDate = LocalDate.now().plusDays(5);

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.countOverlappingRentals(1L, startDate, endDate)).thenReturn(2L);

            boolean result = carAvailabilityService.isCarAvailable(1L, startDate, endDate);

            assertThat(result).isFalse();
            verify(rentalRepository).countOverlappingRentals(1L, startDate, endDate);
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
            when(rentalRepository.findOverlappingRentalsForCar(
                    anyLong(), any(LocalDate.class), any(LocalDate.class), anyList()))
                    .thenReturn(List.of());

            CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L, month);

            assertThat(result).isNotNull();
            assertThat(result.getCarId()).isEqualTo(1L);
            assertThat(result.getMonth()).isEqualTo(month);
            assertThat(result.getDays()).hasSize(month.lengthOfMonth());
            assertThat(result.getCarBlocked()).isFalse();
            assertThat(result.getBlockReason()).isNull();

            verify(carRepository).findByIdAndIsDeletedFalse(1L);
            verify(rentalRepository).findOverlappingRentalsForCar(
                    anyLong(), any(LocalDate.class), any(LocalDate.class), anyList());
        }

        @Test
        @DisplayName("Should mark days as unavailable when rentals exist")
        void shouldMarkDaysAsUnavailableWhenRentalsExist() {
            YearMonth month = YearMonth.now().plusMonths(1);
            LocalDate rentalStart = month.atDay(10);
            LocalDate rentalEnd = month.atDay(15);

            User testUser = User.builder().id(1L).email("test@test.com").build();
            Rental rental = Rental.builder()
                    .id(1L)
                    .car(testCar)
                    .user(testUser)
                    .startDate(rentalStart)
                    .endDate(rentalEnd)
                    .status(RentalStatus.CONFIRMED)
                    .build();

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.findOverlappingRentalsForCar(
                    anyLong(), any(LocalDate.class), any(LocalDate.class), anyList()))
                    .thenReturn(List.of(rental));

            CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L, month);

            assertThat(result.getDays()).hasSize(month.lengthOfMonth());
            
            long unavailableDays = result.getDays().stream()
                    .filter(day -> day.getStatus() == AvailabilityStatus.UNAVAILABLE)
                    .count();
            assertThat(unavailableDays).isEqualTo(6);

            DayAvailabilityDto unavailableDay = result.getDays().stream()
                    .filter(day -> day.getDate().equals(rentalStart))
                    .findFirst()
                    .orElseThrow();
            assertThat(unavailableDay.getStatus()).isEqualTo(AvailabilityStatus.UNAVAILABLE);
            assertThat(unavailableDay.getRentalId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should mark all days unavailable when car has blocking status")
        void shouldMarkAllDaysUnavailableWhenCarHasBlockingStatus() {
            testCar.setCarStatusType(CarStatusType.DAMAGED);
            YearMonth month = YearMonth.now().plusMonths(1);

            when(carRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(testCar));
            when(rentalRepository.findOverlappingRentalsForCar(
                    anyLong(), any(LocalDate.class), any(LocalDate.class), anyList()))
                    .thenReturn(List.of());

            CarAvailabilityCalendarDto result = carAvailabilityService.getCarAvailabilityCalendar(1L, month);

            assertThat(result.getCarBlocked()).isTrue();
            assertThat(result.getBlockReason()).isEqualTo(CarStatusType.DAMAGED.getDisplayName());
            assertThat(result.getDays()).allMatch(day -> day.getStatus() == AvailabilityStatus.UNAVAILABLE);
        }

        @Test
        @DisplayName("Should throw exception when calendar month is in the past")
        void shouldThrowExceptionWhenCalendarMonthIsInThePast() {
            YearMonth pastMonth = YearMonth.now().minusMonths(1);

            assertThatThrownBy(() -> carAvailabilityService.getCarAvailabilityCalendar(1L, pastMonth))
                    .isInstanceOf(RentalValidationException.class)
                    .hasMessageContaining("Calendar month cannot be in the past");

            verify(carRepository, never()).findByIdAndIsDeletedFalse(any());
        }

        @Test
        @DisplayName("Should throw exception when calendar month exceeds 3 months")
        void shouldThrowExceptionWhenCalendarMonthExceeds3Months() {
            YearMonth futureMonth = YearMonth.now().plusMonths(4);

            assertThatThrownBy(() -> carAvailabilityService.getCarAvailabilityCalendar(1L, futureMonth))
                    .isInstanceOf(RentalValidationException.class)
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
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result.getCars()).hasSize(1);
            AvailableCarDto carDto = result.getCars().get(0);
            assertThat(carDto.getDailyRate()).isNotNull();
            assertThat(carDto.getTotalPrice()).isNotNull();
            assertThat(carDto.getCurrency()).isNotNull();
            assertThat(carDto.getAppliedDiscounts()).isNotEmpty();
            assertThat(carDto.getAppliedDiscounts()).contains("Early booking discount");

            verify(dynamicPricingService).calculatePrice(
                    eq(1L), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class));
        }

        @Test
        @DisplayName("Should use dynamic pricing service for price calculation")
        void shouldUseDynamicPricingServiceForPriceCalculation() {
            Page<Car> carPage = new PageImpl<>(List.of(testCar));
            when(carRepository.findAvailableCarsForDateRange(
                    any(LocalDate.class), any(LocalDate.class), anyList(),
                    isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(carPage);
            when(dynamicPricingService.calculatePrice(anyLong(), any(LocalDate.class), 
                    any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(pricingResult);

            AvailabilitySearchResponseDto result = carAvailabilityService.searchAvailableCars(searchRequest);

            assertThat(result.getCars().get(0).getDailyRate()).isEqualTo(pricingResult.effectiveDailyPrice());
            assertThat(result.getCars().get(0).getTotalPrice()).isEqualTo(pricingResult.finalPrice());

            verify(dynamicPricingService).calculatePrice(
                    eq(testCar.getId()), 
                    eq(searchRequest.getStartDate()), 
                    eq(searchRequest.getEndDate()), 
                    any(LocalDate.class));
        }
    }
}
