package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CarRentalApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.server.port=0"})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@DisplayName("Management port")
class ManagementEndpointsTest {

    @LocalServerPort
    private int serverPort;

    @LocalManagementPort
    private int managementPort;

    @Value("${management.endpoints.web.exposure.include}")
    private String exposure;

    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> get(int port, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    @DisplayName("management runs on its own port")
    void separatePort() {
        assertThat(managementPort).isNotEqualTo(serverPort);
    }

    @Test
    @DisplayName("health, liveness and readiness answer UP without details")
    void healthGroups() throws Exception {
        for (String path : new String[] {"/actuator/health", "/actuator/health/liveness",
                "/actuator/health/readiness"}) {
            HttpResponse<String> response = get(managementPort, path);
            assertThat(response.statusCode()).as(path).isEqualTo(200);
            assertThat(response.body()).as(path).contains("\"status\":\"UP\"").doesNotContain("components");
        }
    }

    @Test
    @DisplayName("only health, info and prometheus are exposed")
    void exposure() throws Exception {
        assertThat(exposure).isEqualTo("health,info,prometheus");
        for (String path : new String[] {"/actuator/env", "/actuator/beans", "/actuator/metrics",
                "/actuator/heapdump", "/actuator/loggers"}) {
            assertThat(get(managementPort, path).statusCode()).as(path).isIn(401, 403, 404);
        }
    }

    @Test
    @DisplayName("prometheus metrics are served on the management port")
    void prometheus() throws Exception {
        HttpResponse<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("jvm_memory_used_bytes");
    }

    @Test
    @DisplayName("the application port exposes no actuator endpoint")
    void mainPortHasNoActuator() throws Exception {
        for (String path : new String[] {"/actuator", "/actuator/health", "/actuator/prometheus",
                "/actuator/info", "/health", "/"}) {
            assertThat(get(serverPort, path).statusCode()).as(path).isIn(401, 404);
        }
    }
}
