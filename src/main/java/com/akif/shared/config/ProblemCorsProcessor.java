package com.akif.shared.config;

import com.akif.shared.exception.ErrorCode;
import com.akif.shared.handler.ProblemFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;

class ProblemCorsProcessor extends DefaultCorsProcessor {

    private static final ThreadLocal<String> CURRENT_PATH = new ThreadLocal<>();

    private final ProblemFactory problems;
    private final ObjectMapper objectMapper;

    ProblemCorsProcessor(ProblemFactory problems, ObjectMapper objectMapper) {
        this.problems = problems;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean processRequest(CorsConfiguration config, HttpServletRequest request,
                                  HttpServletResponse response) throws IOException {
        CURRENT_PATH.set(request.getRequestURI());
        try {
            return super.processRequest(config, request, response);
        } finally {
            CURRENT_PATH.remove();
        }
    }

    @Override
    protected void rejectRequest(ServerHttpResponse response) throws IOException {
        ProblemDetail problem = problems.create(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED.name(),
                "Cross-origin request rejected", CURRENT_PATH.get());
        response.setStatusCode(HttpStatus.FORBIDDEN);
        response.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        try {
            response.getBody().write(objectMapper.writeValueAsBytes(problem));
        } catch (RuntimeException e) {
            throw new UncheckedIOException(new IOException(e));
        }
        response.flush();
    }
}
