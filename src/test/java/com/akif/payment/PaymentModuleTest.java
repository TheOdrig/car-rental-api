package com.akif.payment;

import com.akif.payment.api.PaymentService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ApplicationModuleTest(mode = ApplicationModuleTest.BootstrapMode.DIRECT_DEPENDENCIES, extraIncludes = "shared")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Payment module")
class PaymentModuleTest {

    @Autowired
    private PaymentService paymentService;

    @Test
    @DisplayName("bootstraps with its direct dependencies and exposes its public API")
    void bootstraps() {
        assertThat(paymentService).isNotNull();
    }
}
