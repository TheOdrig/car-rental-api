package com.akif.shared.web;

import com.akif.shared.exception.BaseException;
import com.akif.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileController path handling")
class FileControllerPathTest {

    @TempDir
    Path uploads;

    @Test
    @DisplayName("a path that escapes the upload directory is a 400 problem, not an empty response")
    void traversalIsAProblem() {
        FileController controller = new FileController(uploads.toString(), List.of());
        AuthenticatedUser admin = new AuthenticatedUser(1L, "admin", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertThatThrownBy(() -> controller.getAvatar("../secret.jpg", admin))
                .isInstanceOfSatisfying(BaseException.class, ex -> {
                    assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getErrorCode()).isEqualTo("INVALID_PARAMETER");
                });
    }
}
