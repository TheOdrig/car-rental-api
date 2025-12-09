package com.akif.e2e.infrastructure;

import com.akif.dto.request.RentalRequestDto;
import com.akif.shared.enums.AuthProvider;
import com.akif.shared.enums.CarStatusType;
import com.akif.shared.enums.Role;
import com.akif.model.Car;
import com.akif.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    public static RentalRequestDto createRentalRequest(Long carId, LocalDate startDate, LocalDate endDate) {
        return RentalRequestDto.builder()
                .carId(carId)
                .startDate(startDate)
                .endDate(endDate)
                .notes("Test rental request")
                .build();
    }

    public static RentalRequestDto createRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.FUTURE_START, TestFixtures.FUTURE_END);
    }

    public static RentalRequestDto createEarlyBookingRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.EARLY_BOOKING_START, TestFixtures.EARLY_BOOKING_END);
    }

    public static RentalRequestDto createLongDurationRentalRequest(Long carId) {
        return createRentalRequest(carId, TestFixtures.LONG_DURATION_START, TestFixtures.LONG_DURATION_END);
    }

    public static void resetCounter() {
        counter.set(0);
    }
    
    private TestDataBuilder() {}
}
