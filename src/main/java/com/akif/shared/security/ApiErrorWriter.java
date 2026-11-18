package com.akif.shared.security;

import com.akif.shared.handler.ProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final ProblemFactory problems;

    public ApiErrorWriter(ObjectMapper objectMapper, ProblemFactory problems) {
        this.objectMapper = objectMapper;
        this.problems = problems;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String errorCode, String message) throws IOException {
        ProblemDetail body = problems.create(status, errorCode, message, request.getRequestURI());

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
