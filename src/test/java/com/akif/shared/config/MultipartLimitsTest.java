package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Multipart limits follow the configured upload caps")
class MultipartLimitsTest {

    private static final long AVATAR_CAP_BYTES = 5L * 1024 * 1024;

    @Test
    @DisplayName("the per-file limit equals the damage photo cap and covers the avatar cap")
    void fileLimitMatchesCaps() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    DataSize maxFile = DataSize.parse(context.getEnvironment()
                            .getRequiredProperty("spring.servlet.multipart.max-file-size"));
                    long photoCap = Long.parseLong(context.getEnvironment()
                            .getRequiredProperty("damage.max-photo-size-bytes"));

                    assertThat(maxFile.toBytes()).isEqualTo(photoCap);
                    assertThat(maxFile.toBytes()).isGreaterThanOrEqualTo(AVATAR_CAP_BYTES);
                });
    }

    @Test
    @DisplayName("the request limit holds a full set of damage photos")
    void requestLimitHoldsAllPhotos() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .run(context -> {
                    DataSize maxRequest = DataSize.parse(context.getEnvironment()
                            .getRequiredProperty("spring.servlet.multipart.max-request-size"));
                    long photoCap = Long.parseLong(context.getEnvironment()
                            .getRequiredProperty("damage.max-photo-size-bytes"));
                    long photos = Long.parseLong(context.getEnvironment()
                            .getRequiredProperty("damage.max-photos-per-report"));

                    assertThat(maxRequest.toBytes()).isGreaterThanOrEqualTo(photoCap * photos);
                });
    }
}
