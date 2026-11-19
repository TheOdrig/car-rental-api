package com.akif.shared.security;

import com.akif.shared.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Component
public class RequestBodyLimitFilter extends OncePerRequestFilter {

    public static final String MESSAGE = "The request body is too large.";

    private final long maxBytes;
    private final ApiErrorWriter errorWriter;

    public RequestBodyLimitFilter(@Value("${request.max-json-body-bytes:1048576}") long maxBytes,
                                  ApiErrorWriter errorWriter) {
        this.maxBytes = maxBytes;
        this.errorWriter = errorWriter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!isJson(request)) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getContentLengthLong() > maxBytes) {
            errorWriter.write(request, response, HttpStatus.CONTENT_TOO_LARGE,
                    ErrorCode.PAYLOAD_TOO_LARGE.name(), MESSAGE);
            return;
        }
        chain.doFilter(new LimitedRequest(request, maxBytes), response);
    }

    private static boolean isJson(HttpServletRequest request) {
        String type = request.getContentType();
        return type != null && type.toLowerCase(Locale.ROOT).contains("json");
    }

    public static class BodyTooLargeException extends IOException {

        public BodyTooLargeException() {
            super(MESSAGE);
        }
    }

    private static final class LimitedRequest extends HttpServletRequestWrapper {

        private final long max;

        LimitedRequest(HttpServletRequest request, long max) {
            super(request);
            this.max = max;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return new LimitedStream(super.getInputStream(), max);
        }
    }

    private static final class LimitedStream extends ServletInputStream {

        private final ServletInputStream delegate;
        private final long max;
        private long read;

        LimitedStream(ServletInputStream delegate, long max) {
            this.delegate = delegate;
            this.max = max;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) {
                count(1);
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            int count = delegate.read(buffer, offset, length);
            if (count > 0) {
                count(count);
            }
            return count;
        }

        private void count(long bytes) throws IOException {
            read += bytes;
            if (read > max) {
                throw new BodyTooLargeException();
            }
        }

        @Override
        public boolean isFinished() {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady() {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener listener) {
            delegate.setReadListener(listener);
        }
    }
}
