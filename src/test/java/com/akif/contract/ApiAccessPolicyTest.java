package com.akif.contract;

import com.akif.shared.security.Access;
import com.akif.shared.security.ApiAccessPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiAccessPolicy")
class ApiAccessPolicyTest {

    @Test
    @DisplayName("resolves the first matching rule")
    void firstMatchWins() {
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/cars/active")).isEqualTo(Access.PUBLIC);
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/cars/12")).isEqualTo(Access.PUBLIC);
        assertThat(ApiAccessPolicy.resolve("POST", "/api/v1/cars/business/12/maintenance")).isEqualTo(Access.ADMIN);
        assertThat(ApiAccessPolicy.resolve("POST", "/api/v1/rentals/request")).isEqualTo(Access.USER);
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/rentals/me")).isEqualTo(Access.USER);
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/rentals/7")).isEqualTo(Access.AUTHENTICATED);
        assertThat(ApiAccessPolicy.resolve("POST", "/api/v1/webhooks/stripe")).isEqualTo(Access.PUBLIC);
        assertThat(ApiAccessPolicy.resolve("POST", "/api/v1/auth/login")).isEqualTo(Access.PUBLIC);
    }

    @Test
    @DisplayName("falls back to authenticated for unlisted paths")
    void defaultsToAuthenticated() {
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/users/me/profile")).isEqualTo(Access.AUTHENTICATED);
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/cars/licensePlate/34ABC123"))
                .isEqualTo(Access.AUTHENTICATED);
    }

    @Test
    @DisplayName("keeps every admin path admin only")
    void adminPathsAreAdminOnly() {
        assertThat(ApiAccessPolicy.resolve("GET", "/api/v1/admin/anything/at/all")).isEqualTo(Access.ADMIN);
        assertThat(ApiAccessPolicy.resolve("PUT", "/api/v1/cars/3")).isEqualTo(Access.ADMIN);
    }

    @Test
    @DisplayName("resolves templates by substituting path variables")
    void resolvesTemplates() {
        assertThat(ApiAccessPolicy.resolveTemplate("GET", "/api/v1/cars/{id}/similar")).isEqualTo(Access.PUBLIC);
        assertThat(ApiAccessPolicy.resolveTemplate("POST", "/api/v1/rentals/{id}/confirm"))
                .isEqualTo(Access.ADMIN);
    }
}
