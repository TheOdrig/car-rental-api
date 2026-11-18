package com.akif.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Catalogue search access policy")
class CatalogueSearchAccessPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/cars/search",
            "/api/v1/cars/search/brand/Toyota",
            "/api/v1/cars/search/criteria",
            "/api/v1/cars/search/new",
            "/api/v1/cars/search/price-range",
            "/api/v1/cars/search/status/AVAILABLE"
    })
    @DisplayName("every GET catalogue search path is public")
    void getSearchPathsArePublic(String path) {
        assertThat(ApiAccessPolicy.resolve("GET", path)).isEqualTo(Access.PUBLIC);
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    @DisplayName("no write method on the search path is public")
    void writeMethodsAreNotPublic(String method) {
        assertThat(ApiAccessPolicy.resolve(method, "/api/v1/cars/search")).isNotEqualTo(Access.PUBLIC);
    }
}
