package com.akif.e2e.infrastructure;

import com.akif.auth.domain.User;
import com.akif.car.domain.Car;
import com.akif.car.domain.enums.CarStatusType;
import com.akif.rental.domain.enums.RentalStatus;
import com.akif.rental.domain.model.Rental;
import com.akif.rental.internal.dto.request.RentalRequest;
import com.akif.auth.domain.enums.AuthProvider;
import com.akif.shared.enums.CurrencyType;
import com.akif.shared.enums.Role;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TestDataBuilder {
    
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private static final AtomicInteger counter = new AtomicInteger(0);

    public static User createTestUser(String username, Role... roles) {
        int uniqueId = counter.incrementAndGet();
        return User.builder()
                .username(username)
                .email(username + uniqueId + "@test.com")
                .password(passwordEncoder.encode("password123"))
                .authProvider(AuthProvider.LOCAL)
                .roles(roles.length > 0 ? Set.of(roles) : Set.of(Role.USER))
                .enabled(true)
                .isDeleted(false)
                .build();
    }

    public static User createTestUser() {
        return createTestUser(TestFixtures.TEST_USER_USERNAME, Role.USER);
    }

    public static User createTestAdmin() {
        return createTestUser(TestFixtures.TEST_ADMIN_USERNAME, Role.ADMIN);
    }

    public static Car createAvailableCar(String licensePlate, BigDecimal price) {
        int uniqueId = counter.incrementAndGet();
        return Car.builder()
                .licensePlate(licensePlate)
                .vinNumber(TestFixtures.TEST_VIN_PREFIX + uniqueId)
                .brand(TestFixtures.TEST_CAR_BRAND)
                .model(TestFixtures.TEST_CAR_MODEL)
                .productionYear(TestFixtures.TEST_CAR_YEAR)
                .price(price)
                .currencyType(TestFixtures.DEFAULT_CURRENCY)
                .carStatusType(CarStatusType.AVAILABLE)
                .damagePrice(BigDecimal.ZERO)
                .engineType("Gasoline")
                .fuelType("Gasoline")
                .transmissionType("Automatic")
                .bodyType("Sedan")
                .color("White")
                .kilometer(10000L)
                .doors(4)
                .seats(5)
                .isFeatured(false)
                .isTestDriveAvailable(true)
                .viewCount(0L)
                .likeCount(0L)
                .isDeleted(false)
                .build();
    }

    public static Car createAvailableCar() {
        int uniqueId = counter.incrementAndGet();
        return createAvailableCar(
                TestFixtures.TEST_LICENSE_PLATE_PREFIX + uniqueId,
                TestFixtures.BASE_PRICE
        );
    }

    public static RentalRequest createRentalRequest(Long carId, LocalDate startDate, LocalDate endDate) {
        return new RentalRequest(carId, startDate, endDate, "Test rental request");
    }

    public static RentalRequest createRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.FUTURE_START, TestFixtures.FUTURE_END);
    }

    public static RentalRequest createEarlyBookingRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.EARLY_BOOKING_START, TestFixtures.EARLY_BOOKING_END);
    }

    public static RentalRequest createLongDurationRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.LONG_DURATION_START, TestFixtures.LONG_DURATION_END);
    }

    public static void resetCounter() {
        counter.set(0);
    }

    /**
     * Creates a Rental entity with denormalized car and user fields.
     * Use this instead of .car(car).user(user) builder pattern.
     */
    public static Rental createRental(Car car, User user, LocalDate startDate, LocalDate endDate, RentalStatus status) {
        int days = (int) ChronoUnit.DAYS.between(startDate, endDate);
        BigDecimal dailyPrice = car.getPrice();
        BigDecimal totalPrice = dailyPrice.multiply(BigDecimal.valueOf(days));
        
        return Rental.builder()
                .carId(car.getId())
                .carBrand(car.getBrand())
                .carModel(car.getModel())
                .carLicensePlate(car.getLicensePlate())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getUsername())
                .startDate(startDate)
                .endDate(endDate)
                .days(days)
                .dailyPrice(dailyPrice)
                .totalPrice(totalPrice)
                .currency(CurrencyType.TRY)
                .status(status)
                .isDeleted(false)
                .build();
    }

    public static Rental createRental(Car car, User user) {
        return createRental(car, user, TestFixtures.FUTURE_START, TestFixtures.FUTURE_END, RentalStatus.REQUESTED);
    }

    public static Rental createConfirmedRental(Car car, User user) {
        return createRental(car, user, TestFixtures.FUTURE_START, TestFixtures.FUTURE_END, RentalStatus.CONFIRMED);
    }

    public static Rental createInUseRental(Car car, User user) {
        return createRental(car, user, LocalDate.now().minusDays(3), LocalDate.now().plusDays(2), RentalStatus.IN_USE);
    }

    /**
     * Creates a minimal RentalResponse for mocking purposes.
     */
    public static com.akif.rental.api.RentalResponse createRentalResponse(Long id, RentalStatus status) {
        return new com.akif.rental.api.RentalResponse(
                id, null, null, null, null, null, null, null, null, status,
                null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    /**
     * Creates a RentalResponse with basic pricing info for mocking purposes.
     */
    public static com.akif.rental.api.RentalResponse createRentalResponse(Long id, RentalStatus status, 
            java.math.BigDecimal totalPrice, CurrencyType currency) {
        return new com.akif.rental.api.RentalResponse(
                id, null, null, null, null, null, null, totalPrice, currency, status,
                null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
    
    private TestDataBuilder() {}
}
