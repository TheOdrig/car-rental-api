package com.akif.shared.config;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.security.Access;
import com.akif.shared.security.ApiAccessPolicy;
import com.akif.shared.security.ratelimit.RateLimitEndpointClassifier;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
public class ProblemDetailOpenApiCustomizer implements OpenApiCustomizer {

    public static final String PROBLEM_SCHEMA = "ProblemDetail";
    public static final String FIELD_PROBLEM_SCHEMA = "FieldProblem";
    public static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    @Override
    public void customise(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        openApi.getComponents().addSchemas(FIELD_PROBLEM_SCHEMA, fieldProblemSchema());
        openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemSchema());
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, item) -> item.readOperationsMap()
                .forEach((method, operation) -> customise(path, method.name(), operation)));
    }

    private void customise(String path, String method, Operation operation) {
        ApiResponses declared = operation.getResponses() != null ? operation.getResponses() : new ApiResponses();
        Map<String, ApiResponse> sorted = new TreeMap<>(ProblemDetailOpenApiCustomizer::compareCodes);
        sorted.putAll(declared);

        Access access = ApiAccessPolicy.resolveTemplate(method, path);
        boolean secured = access.requiresAuthentication();
        boolean roleRestricted = access == Access.USER || access == Access.ADMIN;
        boolean hasInput = operation.getRequestBody() != null
                || (operation.getParameters() != null && !operation.getParameters().isEmpty());

        if (hasInput) {
            sorted.computeIfAbsent("400", code -> new ApiResponse().description("Invalid request"));
        }
        if (secured) {
            sorted.computeIfAbsent("401", code -> new ApiResponse().description("Authentication required"));
        }
        if (roleRestricted) {
            sorted.computeIfAbsent("403", code -> new ApiResponse().description("Access denied"));
        }
        if (path.contains("{")) {
            sorted.computeIfAbsent("404", code -> new ApiResponse().description("Resource not found"));
        }
        if (RateLimitEndpointClassifier.classifyTemplate(method, path).isPresent()) {
            sorted.computeIfAbsent("429", code -> tooManyRequests());
        }
        sorted.computeIfAbsent("500", code -> new ApiResponse().description("Unexpected error"));

        ApiResponses result = new ApiResponses();
        sorted.forEach((code, response) -> {
            if (isError(code)) {
                response.setContent(problemContent());
            }
            result.addApiResponse(code, response);
        });
        operation.setResponses(result);

        if (!secured) {
            operation.setSecurity(new ArrayList<>());
        }
    }

    private static ApiResponse tooManyRequests() {
        return new ApiResponse()
                .description("Rate limit exceeded; retry after the number of seconds in the Retry-After header")
                .addHeaderObject(HttpHeaders.RETRY_AFTER, new Header()
                        .description("Seconds to wait before the next attempt")
                        .required(true)
                        .schema(new IntegerSchema().format("int64").minimum(BigDecimal.ONE)));
    }

    private static boolean isError(String code) {
        return code.startsWith("4") || code.startsWith("5") || "default".equals(code);
    }

    private static int compareCodes(String left, String right) {
        if ("default".equals(left)) {
            return "default".equals(right) ? 0 : 1;
        }
        if ("default".equals(right)) {
            return -1;
        }
        return left.compareTo(right);
    }

    private static Content problemContent() {
        return new Content().addMediaType(PROBLEM_MEDIA_TYPE,
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA)));
    }

    private static Schema<?> fieldProblemSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.addProperty("field", new StringSchema().description("Request field the message applies to"));
        schema.addProperty("message", new StringSchema().description("Human readable validation message"));
        schema.setRequired(List.of("field", "message"));
        return schema;
    }

    private static Schema<?> problemSchema() {
        Schema<?> schema = new ObjectSchema();
        schema.description("RFC 9457 problem details");
        schema.addProperty("type", new StringSchema().format("uri")
                .description("URN identifying the problem type"));
        schema.addProperty("title", new StringSchema().description("Short summary of the HTTP status"));
        schema.addProperty("status", new IntegerSchema().format("int32")
                .description("HTTP status code"));
        schema.addProperty("detail", new StringSchema().description("Explanation specific to this occurrence"));
        schema.addProperty("instance", new StringSchema().format("uri").description("Request path"));
        schema.addProperty("code", new StringSchema()
                ._enum(Arrays.stream(ErrorCode.values()).map(Enum::name).toList())
                .description("Stable machine readable error code"));
        schema.addProperty("timestamp", new StringSchema().format("date-time").description("UTC time of the error"));
        schema.addProperty("traceId", new StringSchema().description("Correlation id of the request"));
        ArraySchema errors = new ArraySchema();
        errors.items(new Schema<>().$ref("#/components/schemas/" + FIELD_PROBLEM_SCHEMA));
        errors.description("Field level validation errors");
        schema.addProperty("errors", errors);
        Schema<?> details = new ObjectSchema();
        details.additionalProperties(true);
        details.description("Error specific extra data");
        schema.addProperty("details", details);
        schema.setRequired(List.of("type", "title", "status", "detail", "instance", "code", "timestamp"));
        return schema;
    }
}
