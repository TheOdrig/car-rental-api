package com.akif.dashboard.unit;

import com.akif.car.api.CarCatalog;
import com.akif.car.api.CarStatusType;
import com.akif.damage.api.DamageService;
import com.akif.dashboard.api.dto.FleetStatusDto;
import com.akif.dashboard.internal.service.DashboardQueryService;
import com.akif.payment.api.PaymentService;
import com.akif.rental.api.RentalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Fleet status counts distinct cars")
class FleetStatusCalculationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    private final CarCatalog carCatalog = mock(CarCatalog.class);
    private final RentalService rentalService = mock(RentalService.class);
    private final DashboardQueryService service = new DashboardQueryService(
            Clock.fixed(Instant.parse("2026-11-18T12:00:00Z"), ZoneOffset.UTC),
            rentalService, carCatalog, mock(PaymentService.class), mock(DamageService.class));

    private FleetStatusDto fleet(Map<Long, CarStatusType> statuses, Set<Long> inUse, Set<Long> reserved) {
        when(carCatalog.findActiveCarStatuses()).thenReturn(statuses);
        when(rentalService.findCarIdsInUse()).thenReturn(inUse);
        when(rentalService.findCarIdsReservedOn(TODAY)).thenReturn(reserved);
        return service.fetchFleetStatus();
    }

    private static int sum(FleetStatusDto dto) {
        return dto.availableCars() + dto.rentedCars() + dto.reservedCars()
                + dto.maintenanceCars() + dto.inspectionCars() + dto.damagedCars();
    }

    @Test
    @DisplayName("categories add up to the fleet size and occupancy never exceeds 100 percent")
    void categoriesAddUpToTotal() {
        Map<Long, CarStatusType> statuses = Map.of(
                1L, CarStatusType.AVAILABLE,
                2L, CarStatusType.AVAILABLE,
                3L, CarStatusType.AVAILABLE,
                4L, CarStatusType.MAINTENANCE,
                5L, CarStatusType.INSPECTION,
                6L, CarStatusType.DAMAGED);

        FleetStatusDto dto = fleet(statuses, Set.of(1L), Set.of(2L));

        assertThat(dto.totalCars()).isEqualTo(6);
        assertThat(dto.rentedCars()).isEqualTo(1);
        assertThat(dto.reservedCars()).isEqualTo(1);
        assertThat(dto.availableCars()).isEqualTo(1);
        assertThat(dto.maintenanceCars()).isEqualTo(1);
        assertThat(dto.inspectionCars()).isEqualTo(1);
        assertThat(dto.damagedCars()).isEqualTo(1);
        assertThat(sum(dto)).isEqualTo(dto.totalCars());
        assertThat(dto.occupancyRate()).isEqualByComparingTo(new BigDecimal("33.3300"));
    }

    @Test
    @DisplayName("a car with an in-use and a reserved rental is counted once, as in use")
    void carWithSeveralRentalsCountedOnce() {
        FleetStatusDto dto = fleet(Map.of(1L, CarStatusType.AVAILABLE), Set.of(1L), Set.of(1L));

        assertThat(dto.rentedCars()).isEqualTo(1);
        assertThat(dto.reservedCars()).isZero();
        assertThat(dto.availableCars()).isZero();
        assertThat(dto.occupancyRate()).isEqualByComparingTo(new BigDecimal("100"));
        assertThat(sum(dto)).isEqualTo(dto.totalCars());
    }

    @Test
    @DisplayName("a car in maintenance is not also counted as rented")
    void conditionTakesPrecedence() {
        FleetStatusDto dto = fleet(Map.of(1L, CarStatusType.MAINTENANCE), Set.of(1L), Set.of());

        assertThat(dto.maintenanceCars()).isEqualTo(1);
        assertThat(dto.rentedCars()).isZero();
        assertThat(sum(dto)).isEqualTo(dto.totalCars());
    }

    @Test
    @DisplayName("rentals on cars that are no longer in the active fleet are ignored")
    void ignoresRentalsOfRemovedCars() {
        FleetStatusDto dto = fleet(Map.of(1L, CarStatusType.AVAILABLE), Set.of(99L), Set.of(98L));

        assertThat(dto.totalCars()).isEqualTo(1);
        assertThat(dto.rentedCars()).isZero();
        assertThat(dto.reservedCars()).isZero();
        assertThat(dto.availableCars()).isEqualTo(1);
    }

    @Test
    @DisplayName("an empty fleet yields zero occupancy")
    void emptyFleet() {
        FleetStatusDto dto = fleet(Map.of(), Set.of(), Set.of());

        assertThat(dto.totalCars()).isZero();
        assertThat(dto.occupancyRate()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
