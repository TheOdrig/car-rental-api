package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.shared.security.Access;
import com.akif.shared.security.ApiAccessPolicy;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Endpoint inventory")
class EndpointInventoryTest {

    static final Path INVENTORY = Path.of("openapi", "endpoint-inventory.txt");
    private static final Pattern PATH_VARIABLE = Pattern.compile("[{][^}]+[}]");

    record Endpoint(String method, String path, Access access) {
        String line() {
            return method + " " + path + " " + access;
        }
    }

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the committed inventory matches request mappings and the security policy")
    void inventoryMatchesApplication() throws Exception {
        String expected = collect().stream().map(Endpoint::line).collect(Collectors.joining("\n", "", "\n"));

        ContractSnapshots.verify(INVENTORY, expected, "inventory.update",
                "./mvnw -B test -Dtest=EndpointInventoryTest -Dinventory.update=true");
    }

    @Test
    @DisplayName("every versioned endpoint lives under /api/v1")
    void everyEndpointIsVersioned() {
        assertThat(collect()).allSatisfy(endpoint -> assertThat(endpoint.path()).startsWith("/api/v1/"));
    }

    @Test
    @DisplayName("the security chain enforces the access level recorded in the inventory")
    void securityChainEnforcesInventory() throws Exception {
        for (Endpoint endpoint : collect()) {
            String path = PATH_VARIABLE.matcher(endpoint.path()).replaceAll("1");
            int anonymous = send(endpoint, path, null);
            if (endpoint.access() == Access.PUBLIC) {
                assertThat(anonymous).as("%s should be reachable anonymously", endpoint.line()).isNotEqualTo(401);
            } else {
                assertThat(anonymous).as("%s should require authentication", endpoint.line()).isEqualTo(401);
            }
            if (endpoint.access() == Access.ADMIN) {
                assertThat(send(endpoint, path, "ROLE_USER"))
                        .as("%s should reject a plain user", endpoint.line()).isEqualTo(403);
            }
            if (endpoint.access() == Access.USER) {
                assertThat(send(endpoint, path, "ROLE_ADMIN"))
                        .as("%s should reject an admin without the user role", endpoint.line()).isEqualTo(403);
            }
        }
    }

    private int send(Endpoint endpoint, String path, String authority) throws Exception {
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(endpoint.method()), path);
        if (authority != null) {
            builder = builder.with(jwt().authorities(new SimpleGrantedAuthority(authority)));
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    private List<Endpoint> collect() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(entry -> entry.getValue().getBeanType().getName().startsWith("com.akif."))
                .flatMap(entry -> expand(entry.getKey(), entry.getValue()))
                .distinct()
                .sorted(Comparator.comparing(Endpoint::path).thenComparing(Endpoint::method))
                .toList();
    }

    private Stream<Endpoint> expand(RequestMappingInfo mapping, HandlerMethod handler) {
        Set<String> patterns = mapping.getPathPatternsCondition().getPatternValues();
        Set<String> methods = mapping.getMethodsCondition().getMethods().stream()
                .map(Enum::name).collect(Collectors.toSet());
        assertThat(methods).as("%s must declare an HTTP method", handler).isNotEmpty();
        return patterns.stream().flatMap(pattern -> methods.stream()
                .map(method -> new Endpoint(method, pattern,
                        effectiveAccess(ApiAccessPolicy.resolveTemplate(method, pattern), handler))));
    }

    private static Access effectiveAccess(Access urlAccess, HandlerMethod handler) {
        PreAuthorize methodRule = handler.getMethodAnnotation(PreAuthorize.class);
        PreAuthorize rule = methodRule != null ? methodRule
                : AnnotatedElementUtils.findMergedAnnotation(handler.getBeanType(), PreAuthorize.class);
        if (rule == null || urlAccess == Access.PUBLIC) {
            return urlAccess;
        }
        if (rule.value().contains("hasRole('ADMIN')")) {
            return Access.ADMIN;
        }
        if (rule.value().contains("hasRole('USER')") && urlAccess == Access.AUTHENTICATED) {
            return Access.USER;
        }
        return urlAccess;
    }
}
