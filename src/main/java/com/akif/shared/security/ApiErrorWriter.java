package com.akif.shared.security;

import com.akif.shared.handler.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class ApiErrorWriter {

    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ApiErrorWriter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void write(HttpServletRequest request, HttpServletResponse response,
                      HttpStatus status, String errorCode, String message) throws IOException {
        String correlationId = MDC.get("correlationId");
        ErrorResponseDto body = ErrorResponseDto.builder()
                .errorCode(errorCode)
                .message(message)
                .timestamp(LocalDateTime.now(clock))
                .status(status.value())
                .path(request.getRequestURI())
                .traceId(correlationId != null ? correlationId : UUID.randomUUID().toString())
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
