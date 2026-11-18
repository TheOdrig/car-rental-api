package com.akif.contract;

import com.akif.CarRentalApplication;
import com.akif.shared.exception.ErrorCode;
import com.akif.shared.security.ratelimit.RateLimitEndpointClassifier;
import com.akif.testsupport.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@SpringBootTest(classes = CarRentalApplication.class)
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("OpenAPI contract")
class OpenApiContractTest {

    static final Path ARTIFACT = Path.of("openapi", "openapi.json");
    private static final List<String> HTTP_METHODS = List.of("get", "post", "put", "patch", "delete");

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("the committed artifact matches the generated specification")
    void artifactMatchesGeneratedSpec() throws Exception {
        ContractSnapshots.verify(ARTIFACT, generate(), "openapi.update",
                "./mvnw -B test -Dtest=OpenApiContractTest -Dopenapi.update=true");
    }

    @Test
    @DisplayName("generation is deterministic")
    void generationIsDeterministic() throws Exception {
        assertThat(generate()).isEqualTo(generate());
    }

    @Test
    @DisplayName("the specification does not depend on the environment")
    void specificationIsEnvironmentIndependent() throws Exception {
        JsonNode spec = mapper.readTree(generate());

        assertThat(spec.path("servers")).hasSize(1);
        assertThat(spec.path("servers").get(0).path("url").asString()).isEqualTo("/");
        assertThat(generate()).doesNotContain("localhost");
    }

    @Test
    @DisplayName("every versioned path documents ProblemDetail error responses")
    void everyOperationDocumentsProblemErrors() throws Exception {
        JsonNode spec = mapper.readTree(generate());
        List<String> failures = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : spec.path("paths").properties()) {
            if (!path.getKey().startsWith("/api/v1/")) {
                failures.add("unversioned path " + path.getKey());
            }
            for (String method : HTTP_METHODS) {
                JsonNode operation = path.getValue().path(method);
                if (operation.isMissingNode()) {
                    continue;
                }
                String where = method.toUpperCase() + " " + path.getKey();
                boolean hasError = false;
                for (Map.Entry<String, JsonNode> response : operation.path("responses").properties()) {
                    String code = response.getKey();
                    if (code.startsWith("4") || code.startsWith("5")) {
                        hasError = true;
                        String ref = response.getValue().path("content").path("application/problem+json")
                                .path("schema").path("$ref").asString("");
                        if (!ref.equals("#/components/schemas/ProblemDetail")) {
                            failures.add(where + " " + code + " is not a ProblemDetail response");
                        }
                    }
                }
                if (!hasError || operation.path("responses").path("500").isMissingNode()) {
                    failures.add(where + " documents no error responses");
                }
            }
        }
        assertThat(failures).isEmpty();
    }

    @Test
    @DisplayName("every rate limited operation documents a 429 ProblemDetail with Retry-After")
    void rateLimitedOperationsDocument429() throws Exception {
        JsonNode spec = mapper.readTree(generate());
        List<String> failures = new ArrayList<>();
        List<String> limited = new ArrayList<>();
        for (Map.Entry<String, JsonNode> path : spec.path("paths").properties()) {
            for (String method : HTTP_METHODS) {
                JsonNode operation = path.getValue().path(method);
                if (operation.isMissingNode()) {
                    continue;
                }
                JsonNode tooMany = operation.path("responses").path("429");
                boolean rateLimited = RateLimitEndpointClassifier
                        .classifyTemplate(method.toUpperCase(), path.getKey()).isPresent();
                String where = method.toUpperCase() + " " + path.getKey();
                if (rateLimited) {
                    limited.add(where);
                    if (!tooMany.path("content").path("application/problem+json").path("schema").path("$ref")
                            .asString("").equals("#/components/schemas/ProblemDetail")
                            || tooMany.path("headers").path("Retry-After").isMissingNode()) {
                        failures.add(where + " lacks a 429 ProblemDetail with Retry-After");
                    }
                } else if (!tooMany.isMissingNode()) {
                    failures.add(where + " documents 429 but is not rate limited");
                }
            }
        }
        assertThat(limited).hasSize(RateLimitEndpointClassifier.RULES.size());
        assertThat(failures).isEmpty();
    }

    @Test
    @DisplayName("the ProblemDetail code property enumerates exactly the ErrorCode values")
    void problemCodeEnumMatchesErrorCodes() throws Exception {
        JsonNode codes = mapper.readTree(generate()).path("components").path("schemas").path("ProblemDetail")
                .path("properties").path("code").path("enum");
        List<String> documented = new ArrayList<>();
        codes.forEach(code -> documented.add(code.asString()));

        assertThat(documented).containsExactlyInAnyOrderElementsOf(
                Stream.of(ErrorCode.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("paged responses use the stable PageResponse schema only")
    void pagedResponsesAreStable() throws Exception {
        JsonNode schemas = mapper.readTree(generate()).path("components").path("schemas");
        List<String> names = new ArrayList<>();
        schemas.propertyNames().forEach(names::add);

        assertThat(names).noneMatch(name -> name.startsWith("Page") && !name.startsWith("PageResponse"));
        assertThat(names).noneMatch(name -> name.equals("Pageable") || name.equals("PageableObject")
                || name.equals("SortObject"));
        assertThat(names).anyMatch(name -> name.startsWith("PageResponse"));
        assertThat(names).contains("ProblemDetail", "FieldProblem");
    }

    private String generate() throws Exception {
        String raw = mockMvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode sorted = sort(mapper.readTree(raw));
        return ContractSnapshots.normalize(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(sorted)) + "\n";
    }

    private JsonNode sortedStrings(JsonNode node) {
        if (!node.isArray()) {
            return node;
        }
        List<String> values = new ArrayList<>();
        node.forEach(element -> values.add(element.asString()));
        values.sort(String::compareTo);
        ArrayNode result = mapper.createArrayNode();
        values.forEach(result::add);
        return result;
    }

    private JsonNode sort(JsonNode node) {
        if (node.isObject()) {
            Map<String, JsonNode> ordered = new TreeMap<>();
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                JsonNode value = sort(entry.getValue());
                ordered.put(entry.getKey(), entry.getKey().equals("required") ? sortedStrings(value) : value);
            }
            ObjectNode result = mapper.createObjectNode();
            ordered.forEach(result::set);
            return result;
        }
        if (node.isArray()) {
            ArrayNode result = mapper.createArrayNode();
            node.forEach(element -> result.add(sort(element)));
            return result;
        }
        return node;
    }
}
