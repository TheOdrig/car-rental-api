package com.akif.service.currency;

import com.akif.dto.currency.ConversionResult;
import com.akif.dto.currency.ExchangeRate;
import com.akif.dto.currency.ExchangeRateResponse;
import com.akif.dto.currency.ExchangeRatesResponse;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.RateSource;
import com.akif.service.currency.impl.CurrencyConversionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CurrencyConversionService Unit Tests")
class CurrencyConversionServiceImplTest {

    @Mock
    private IExchangeRateCacheService cacheService;

    @InjectMocks
    private CurrencyConversionServiceImpl currencyConversionService;

    private ExchangeRateResponse mockRateResponse;
    private LocalDateTime testTimestamp;

    @BeforeEach
    void setUp() {
        testTimestamp = LocalDateTime.now();
        
        Map<CurrencyType, BigDecimal> rates = Map.of(
                CurrencyType.USD, BigDecimal.ONE,
                CurrencyType.TRY, new BigDecimal("34.50"),
                CurrencyType.EUR, new BigDecimal("0.92"),
                CurrencyType.GBP, new BigDecimal("0.79"),
                CurrencyType.JPY, new BigDecimal("149.50")
        );
        
        mockRateResponse = new ExchangeRateResponse(
                CurrencyType.USD,
                testTimestamp,
                rates,
                RateSource.LIVE
        );
    }

    @Nested
    @DisplayName("Convert Operations")
    class ConvertOperations {

        @Test
        @DisplayName("Should return same amount when converting same currency")
        void shouldReturnSameAmountWhenConvertingSameCurrency() {
            BigDecimal amount = new BigDecimal("100.00");

            ConversionResult result = currencyConversionService.convert(amount, CurrencyType.USD, CurrencyType.USD);

            assertThat(result).isNotNull();
            assertThat(result.originalAmount()).isEqualByComparingTo(amount);
            assertThat(result.convertedAmount()).isEqualByComparingTo(amount);
            assertThat(result.exchangeRate()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.originalCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(result.targetCurrency()).isEqualTo(CurrencyType.USD);
            
            verifyNoInteractions(cacheService);
        }

        @Test
        @DisplayName("Should convert USD to TRY correctly")
        void shouldConvertUsdToTryCorrectly() {
            BigDecimal amount = new BigDecimal("100.00");
            
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ConversionResult result = currencyConversionService.convert(amount, CurrencyType.USD, CurrencyType.TRY);

            assertThat(result).isNotNull();
            assertThat(result.originalAmount()).isEqualByComparingTo(amount);
            assertThat(result.convertedAmount()).isEqualByComparingTo(new BigDecimal("3450.00"));
            assertThat(result.exchangeRate()).isEqualByComparingTo(new BigDecimal("34.50"));
            assertThat(result.source()).isEqualTo(RateSource.LIVE);
            
            verify(cacheService).getCachedRates(CurrencyType.USD);
        }

        @Test
        @DisplayName("Should convert USD to EUR correctly")
        void shouldConvertUsdToEurCorrectly() {
            BigDecimal amount = new BigDecimal("100.00");
            
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ConversionResult result = currencyConversionService.convert(amount, CurrencyType.USD, CurrencyType.EUR);

            assertThat(result).isNotNull();
            assertThat(result.convertedAmount()).isEqualByComparingTo(new BigDecimal("92.00"));
            assertThat(result.exchangeRate()).isEqualByComparingTo(new BigDecimal("0.92"));
        }

        @Test
        @DisplayName("Should apply HALF_UP rounding for 2 decimal places")
        void shouldApplyHalfUpRoundingForTwoDecimalPlaces() {
            BigDecimal amount = new BigDecimal("33.33");
            
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ConversionResult result = currencyConversionService.convert(amount, CurrencyType.USD, CurrencyType.TRY);

            assertThat(result.convertedAmount()).isEqualByComparingTo(new BigDecimal("1149.89"));
        }

        @Test
        @DisplayName("Should apply 0 decimal places for JPY")
        void shouldApplyZeroDecimalPlacesForJpy() {
            BigDecimal amount = new BigDecimal("100.00");
            
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ConversionResult result = currencyConversionService.convert(amount, CurrencyType.USD, CurrencyType.JPY);

            assertThat(result.convertedAmount()).isEqualByComparingTo(new BigDecimal("14950"));
            assertThat(result.convertedAmount().scale()).isLessThanOrEqualTo(0);
        }
    }

    @Nested
    @DisplayName("GetRate Operations")
    class GetRateOperations {

        @Test
        @DisplayName("Should return rate 1.0 for same currency")
        void shouldReturnRateOneForSameCurrency() {
            ExchangeRate rate = currencyConversionService.getRate(CurrencyType.EUR, CurrencyType.EUR);

            assertThat(rate).isNotNull();
            assertThat(rate.rate()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(rate.from()).isEqualTo(CurrencyType.EUR);
            assertThat(rate.to()).isEqualTo(CurrencyType.EUR);
            
            verifyNoInteractions(cacheService);
        }

        @Test
        @DisplayName("Should return correct rate from cache")
        void shouldReturnCorrectRateFromCache() {
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ExchangeRate rate = currencyConversionService.getRate(CurrencyType.USD, CurrencyType.TRY);

            assertThat(rate).isNotNull();
            assertThat(rate.rate()).isEqualByComparingTo(new BigDecimal("34.50"));
            assertThat(rate.source()).isEqualTo(RateSource.LIVE);
            assertThat(rate.timestamp()).isEqualTo(testTimestamp);
        }
    }

    @Nested
    @DisplayName("GetAllRates Operations")
    class GetAllRatesOperations {

        @Test
        @DisplayName("Should return all rates from cache")
        void shouldReturnAllRatesFromCache() {
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            ExchangeRatesResponse response = currencyConversionService.getAllRates();

            assertThat(response).isNotNull();
            assertThat(response.baseCurrency()).isEqualTo(CurrencyType.USD);
            assertThat(response.rates()).containsKeys(CurrencyType.USD, CurrencyType.TRY, CurrencyType.EUR, CurrencyType.GBP, CurrencyType.JPY);
            assertThat(response.source()).isEqualTo(RateSource.LIVE);
        }
    }

    @Nested
    @DisplayName("RefreshRates Operations")
    class RefreshRatesOperations {

        @Test
        @DisplayName("Should evict cache and fetch new rates")
        void shouldEvictCacheAndFetchNewRates() {
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(mockRateResponse);

            currencyConversionService.refreshRates();

            verify(cacheService).evictCache();
            verify(cacheService).getCachedRates(CurrencyType.USD);
        }
    }

    @Nested
    @DisplayName("Fallback Behavior")
    class FallbackBehavior {

        @Test
        @DisplayName("Should return FALLBACK source when using fallback rates")
        void shouldReturnFallbackSourceWhenUsingFallbackRates() {
            ExchangeRateResponse fallbackResponse = new ExchangeRateResponse(
                    CurrencyType.USD,
                    testTimestamp,
                    Map.of(CurrencyType.USD, BigDecimal.ONE, CurrencyType.TRY, new BigDecimal("34.00")),
                    RateSource.FALLBACK
            );
            
            when(cacheService.getCachedRates(CurrencyType.USD)).thenReturn(fallbackResponse);

            ConversionResult result = currencyConversionService.convert(
                    new BigDecimal("100"), CurrencyType.USD, CurrencyType.TRY);

            assertThat(result.source()).isEqualTo(RateSource.FALLBACK);
        }
    }
}
