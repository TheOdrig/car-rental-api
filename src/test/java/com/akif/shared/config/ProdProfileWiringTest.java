package com.akif.shared.config;

import com.akif.CarRentalApplication;
import com.akif.notification.internal.service.email.EmailSender;
import com.akif.notification.internal.service.email.impl.MockEmailSender;
import com.akif.notification.internal.service.email.impl.SendGridEmailSender;
import com.akif.payment.internal.service.gateway.PaymentGateway;
import com.akif.payment.internal.service.gateway.impl.StripePaymentGateway;
import com.akif.payment.internal.service.gateway.impl.StubPaymentGateway;
import com.akif.shared.infrastructure.FileUploadService;
import com.akif.shared.infrastructure.LocalFileStorageService;
import com.akif.shared.infrastructure.R2FileStorageService;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class, properties = {
        "jwt.secret=k7Hq2xVn9Zp4Lm8Rt3Wd6Yb1Cs5Fg0Je",
        "oauth2.state.secret=Qw3rTy7uIo9pAs2dFg5hJk8lZx1cVb4n",
        "cors.allowed-origins=https://rent.example.com",
        "stripe.api.key=sk_test_51Habcdefghijklmnop",
        "stripe.webhook.secret=whsec_abcdefghijklmnop",
        "stripe.metadata.environment=prod",
        "r2.endpoint=https://acc.r2.cloudflarestorage.com",
        "r2.bucket-name=bucket",
        "r2.access-key-id=id",
        "r2.secret-access-key=key",
        "email.from=noreply@example.com",
        "spring.mail.password=mail-key",
        "exchange-rate.api.key=rate-key",
        "oauth2.google.client-id=g-id",
        "oauth2.google.client-secret=g-secret",
        "oauth2.github.client-id=gh-id",
        "oauth2.github.client-secret=gh-secret",
        "security.rate-limit.trust-forwarded-headers=true",
        "security.rate-limit.trusted-proxy-hops=1",
        "security.rate-limit.trusted-proxy-cidrs=172.16.0.0/12",
        "spring.datasource.password=ignored",
        "spring.datasource.username=ignored",
        "spring.datasource.url=jdbc:postgresql://ignored/ignored",
        "APP_BASE_URL=https://rent.example.com"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("prod")
@DisplayName("Prod profile wiring")
class ProdProfileWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("resolves the real payment, email and file storage implementations")
    void resolvesRealImplementations() {
        assertThat(context.getBean(PaymentGateway.class)).isInstanceOf(StripePaymentGateway.class);
        assertThat(context.getBean(EmailSender.class)).isInstanceOf(SendGridEmailSender.class);
        assertThat(context.getBean(FileUploadService.class)).isInstanceOf(R2FileStorageService.class);
    }

    @Test
    @DisplayName("contains no stub beans")
    void containsNoStubBeans() {
        assertThat(context.getBeansOfType(StubPaymentGateway.class)).isEmpty();
        assertThat(context.getBeansOfType(MockEmailSender.class)).isEmpty();
        assertThat(context.getBeansOfType(LocalFileStorageService.class)).isEmpty();
    }

    @Test
    @DisplayName("does not expose the API documentation")
    void doesNotExposeApiDocs() {
        assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled")).isEqualTo("false");
        assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled")).isEqualTo("false");
        assertThat(context.getBeanNamesForType(org.springdoc.webmvc.api.OpenApiWebMvcResource.class)).isEmpty();
    }

    @Test
    @DisplayName("registers the production configuration validator as an environment post processor")
    void validatesConfigurationAtStartup() {
        try {
            java.util.List<String> declared = new java.util.ArrayList<>();
            for (java.net.URL url : java.util.Collections.list(getClass().getClassLoader().getResources("META-INF/spring.factories"))) {
                try (java.io.InputStream stream = url.openStream()) {
                    declared.add(new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            assertThat(String.join("
", declared))
                    .contains("org.springframework.boot.EnvironmentPostProcessor=" + ProdConfigurationValidator.class.getName());
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }
}
