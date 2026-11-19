package com.akif.auth.integration;

import java.time.Duration;
import com.akif.CarRentalApplication;
import com.akif.auth.domain.PasswordResetToken;
import com.akif.auth.internal.repository.PasswordResetTokenRepository;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("PasswordResetTokenRepository")
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository repository;

    @Test
    @DisplayName("unused tokens are removed whatever the case of the stored or requested email")
    void deletesUnusedTokensCaseInsensitively() {
        repository.save(PasswordResetToken.builder()
                .token("token-mixed-case")
                .email("Mixed.Case@Example.com")
                .expiryDate(Instant.now().plus(Duration.ofHours(1)))
                .build());
        repository.flush();

        repository.deleteUnusedTokensByEmail("mixed.case@example.com");
        repository.flush();

        assertThat(repository.findByToken("token-mixed-case")).isEmpty();
    }
}
