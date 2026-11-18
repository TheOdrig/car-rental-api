package com.akif.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

@Getter
public class RateLimitExceededException extends BaseException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super(ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "Too many requests. Retry after " + retryAfterSeconds + " seconds.",
                HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfterSeconds;
        setDetails(Map.of("retryAfterSeconds", retryAfterSeconds));
    }
}
