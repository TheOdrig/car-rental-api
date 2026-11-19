package com.akif.notification.unit;

import java.time.ZoneOffset;
import java.time.LocalDateTime;
import com.akif.notification.internal.service.email.impl.EmailTemplateServiceImpl;
import com.akif.rental.api.PaymentRetryRequestedEvent;
import com.akif.shared.enums.CurrencyType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRetryEmailLinkTest {

    @Test
    void rendersPayLinkForTheRental() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        EmailTemplateServiceImpl service = new EmailTemplateServiceImpl(engine);
        ReflectionTestUtils.setField(service, "baseUrl", "https://rent.example.com");

        String html = service.renderPaymentRetryEmail(new PaymentRetryRequestedEvent(
                42L, "customer@example.com", LocalDateTime.of(2026, 1, 1, 10, 0).toInstant(ZoneOffset.UTC),
                "Toyota", "Corolla", LocalDate.of(2026, 2, 1), new BigDecimal("120.00"), CurrencyType.USD));

        assertThat(html).contains("href=\"https://rent.example.com/rentals/42/pay\"");
    }
}
