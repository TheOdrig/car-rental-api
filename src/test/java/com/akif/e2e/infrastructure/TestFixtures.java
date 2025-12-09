package com.akif.e2e.infrastructure;

import com.akif.shared.enums.CurrencyType;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TestFixtures {

    public static final BigDecimal BASE_PRICE = new BigDecimal("500.00");

    public static final CurrencyType DEFAULT_CURRENCY = CurrencyType.TRY;

    public static final LocalDate FUTURE_START = LocalDate.now().plusDays(1);

    public static final LocalDate FUTURE_END = LocalDate.now().plusDays(5);

    public static final LocalDate EARLY_BOOKING_START = LocalDate.now().plusDays(35);

    public static final LocalDate EARLY_BOOKING_END = LocalDate.now().plusDays(40);

    public static final LocalDate LONG_DURATION_START = LocalDate.now().plusDays(1);

    public static final LocalDate LONG_DURATION_END = LocalDate.now().plusDays(15);

    public static final String TEST_USER_USERNAME = "testuser";

    public static final String TEST_USER_EMAIL = "testuser@example.com";

    public static final String TEST_USER_PASSWORD = "password123";

    public static final String TEST_ADMIN_USERNAME = "testadmin";

    public static final String TEST_ADMIN_EMAIL = "testadmin@example.com";

    public static final String TEST_ADMIN_PASSWORD = "admin123";

    public static final String TEST_LICENSE_PLATE_PREFIX = "34TEST";

    public static final String TEST_VIN_PREFIX = "TEST";

    public static final String TEST_CAR_BRAND = "Toyota";

    public static final String TEST_CAR_MODEL = "Corolla";

    public static final Integer TEST_CAR_YEAR = 2023;
    
    private TestFixtures() {}
}
