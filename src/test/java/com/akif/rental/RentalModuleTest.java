package com.akif.rental;

import com.akif.rental.api.RentalService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "shared")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Rental module")
class RentalModuleTest {

    @MockitoBean
    private com.akif.auth.api.UserDamageStatisticsProvider damageStatistics;

    @Autowired
    private RentalService rentalService;

    @Test
    @DisplayName("bootstraps with its direct dependencies and exposes its public API")
    void bootstraps() {
        assertThat(rentalService).isNotNull();
    }
}
