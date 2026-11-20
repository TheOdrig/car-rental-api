package com.akif.dashboard.unit;

import com.akif.damage.api.DamageService;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.api.dto.PendingItemDto;
import com.akif.dashboard.domain.model.Alert;
import com.akif.dashboard.internal.mapper.DashboardMapper;
import com.akif.dashboard.internal.repository.AlertRepository;
import com.akif.dashboard.internal.service.AlertServiceImpl;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.payment.api.PaymentService;
import com.akif.car.api.CarCatalog;
import com.akif.rental.api.LateHoursPolicy;
import com.akif.rental.api.RentalResponse;
import com.akif.rental.api.RentalService;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.config.PenaltyConfig;
import com.akif.testsupport.ConfigFixtures;
import com.akif.rental.internal.service.penalty.impl.LateHoursPolicyImpl;
import com.akif.rental.internal.service.penalty.impl.PenaltyCalculationServiceImpl;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Dashboard, alert and penalty agree on late hours")
class LateHoursAgreementTest {

    private static final LocalDate END_DATE = LocalDate.of(2026, 11, 17);

    @ParameterizedTest(name = "{0} -> {1} h")
    @CsvSource({
            "2026-11-18T00:15:00Z, 0",
            "2026-11-18T01:01:00Z, 1",
            "2026-11-18T12:00:00Z, 11",
            "2026-11-18T23:59:00Z, 23",
            "2026-11-19T00:15:00Z, 24",
            "2026-11-19T01:01:00Z, 25"
    })
    void allThreeMeasureTheSameLateHours(String instant, long expectedHours) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        PenaltyConfig config = ConfigFixtures.penaltyConfig();
        LateHoursPolicy policy = new LateHoursPolicyImpl(config);

        Rental rental = Rental.builder()
                .id(1L)
                .endDate(END_DATE)
                .dailyPrice(new BigDecimal("500.00"))
                .currency(CurrencyType.TRY)
                .build();
        int penaltyHours = new PenaltyCalculationServiceImpl(config)
                .calculatePenalty(rental, Instant.now(clock)).lateHours();

        RentalResponse response = RentalResponse.builder().id(1L).endDate(END_DATE).build();
        RentalService rentalService = mock(RentalService.class);
        when(rentalService.findOverdueRentals(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response)));

        DashboardQueryService dashboard = new DashboardQueryService(
                clock, policy, rentalService, mock(CarCatalog.class), mock(PaymentService.class), mock(DamageService.class));
        PendingItemDto item = dashboard.fetchOverdueRentals(Pageable.unpaged()).getContent().get(0);

        AlertRepository alertRepository = mock(AlertRepository.class);
        DashboardQueryService alertDashboard = mock(DashboardQueryService.class);
        when(alertDashboard.fetchFleetStatus())
                .thenReturn(new FleetStatusDto(100, 50, 0, 0, 0, 0, 0, BigDecimal.ZERO, Instant.now()));
        AlertServiceImpl alerts = new AlertServiceImpl(
                clock, policy, alertRepository, mock(DashboardMapper.class), rentalService,
                alertDashboard, mock(DamageService.class), mock(PaymentService.class));
        when(alertRepository.save(any(Alert.class))).thenAnswer(call -> call.getArgument(0));
        alerts.generateAlerts();

        assertThat(penaltyHours).isEqualTo(expectedHours);
        assertThat(item.lateHours()).isEqualTo(expectedHours);
        if (expectedHours >= 24) {
            ArgumentCaptor<Alert> saved = ArgumentCaptor.forClass(Alert.class);
            verify(alertRepository, times(1)).save(saved.capture());
            assertThat(saved.getValue().getMessage()).contains("is " + expectedHours + " hours overdue");
        } else {
            verify(alertRepository, never()).save(any(Alert.class));
        }
    }
}
