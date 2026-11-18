package com.akif.shared.handler;

import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProblemFactory {

    public static final String TYPE_PREFIX = "urn:car-rental:problem:";
    public static final String PROP_CODE = "code";
    public static final String PROP_TIMESTAMP = "timestamp";
    public static final String PROP_TRACE_ID = "traceId";
    public static final String PROP_ERRORS = "errors";
    public static final String PROP_DETAILS = "details";

    private final Clock clock;

    public ProblemDetail create(HttpStatusCode status, String code, String detail, String instance) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
        problem.setTitle(title(status));
        problem.setInstance(toUri(instance));
        problem.setProperty(PROP_CODE, code);
        problem.setProperty(PROP_TIMESTAMP, clock.instant());
        problem.setProperty(PROP_TRACE_ID, traceId());
        return problem;
    }

    public ProblemDetail create(HttpStatusCode status, String code, String detail, String instance,
                                List<FieldProblem> errors) {
        ProblemDetail problem = create(status, code, detail, instance);
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty(PROP_ERRORS, errors);
        }
        return problem;
    }

    public ProblemDetail create(HttpStatusCode status, String code, String detail, String instance,
                                Map<String, Object> details) {
        ProblemDetail problem = create(status, code, detail, instance);
        if (details != null && !details.isEmpty()) {
            problem.setProperty(PROP_DETAILS, details);
        }
        return problem;
    }

    public ProblemDetail enrich(ProblemDetail problem, String code, String instance) {
        if (problem.getTitle() == null) {
            problem.setTitle(title(HttpStatusCode.valueOf(problem.getStatus())));
        }
        if (problem.getInstance() == null) {
            problem.setInstance(toUri(instance));
        }
        if (problem.getProperties() == null || !problem.getProperties().containsKey(PROP_CODE)) {
            problem.setType(URI.create(TYPE_PREFIX + code.toLowerCase(Locale.ROOT).replace('_', '-')));
            problem.setProperty(PROP_CODE, code);
        }
        if (problem.getProperties() == null || !problem.getProperties().containsKey(PROP_TIMESTAMP)) {
            problem.setProperty(PROP_TIMESTAMP, clock.instant());
        }
        if (problem.getProperties() == null || !problem.getProperties().containsKey(PROP_TRACE_ID)) {
            problem.setProperty(PROP_TRACE_ID, traceId());
        }
        return problem;
    }

    public String traceId() {
        String correlationId = MDC.get("correlationId");
        return correlationId != null ? correlationId : UUID.randomUUID().toString();
    }

    private static String title(HttpStatusCode status) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        return resolved != null ? resolved.getReasonPhrase() : "Error";
    }

    private static URI toUri(String instance) {
        if (instance == null) {
            return null;
        }
        try {
            return URI.create(instance);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
