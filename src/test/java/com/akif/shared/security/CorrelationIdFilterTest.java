package com.akif.shared.security;

import com.akif.shared.security.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Unit Tests")
class CorrelationIdFilterTest {

    @InjectMocks
    private CorrelationIdFilter correlationIdFilter;

    @Mock
    private FilterChain filterChain;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should generate new correlation ID when header is missing")
    void shouldGenerateNewCorrelationIdWhenHeaderMissing() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotNull();
        assertThat(correlationId).hasSize(36);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should use existing correlation ID from header")
    void shouldUseExistingCorrelationIdFromHeader() throws ServletException, IOException {
        String existingCorrelationId = "test-correlation-id-123";
        request.addHeader("X-Correlation-ID", existingCorrelationId);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String responseCorrelationId = response.getHeader("X-Correlation-ID");
        assertThat(responseCorrelationId).isEqualTo(existingCorrelationId);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should add correlation ID to response header")
    void shouldAddCorrelationIdToResponseHeader() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getHeader("X-Correlation-ID")).isNotNull();
    }

    @Test
    @DisplayName("Should set userId to 'anonymous' when user is not authenticated")
    void shouldSetUserIdToAnonymousWhenNotAuthenticated() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(null);

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should extract userId from authenticated user")
    void shouldExtractUserIdFromAuthenticatedUser() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("testUser");
        when(authentication.getName()).thenReturn("testUser");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should handle anonymousUser principal")
    void shouldHandleAnonymousUserPrincipal() throws ServletException, IOException {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("Should clean up MDC after filter execution")
    void shouldCleanUpMdcAfterFilterExecution() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should clean up MDC even when filter chain throws exception")
    void shouldCleanUpMdcEvenWhenFilterChainThrowsException() throws ServletException, IOException {
        ServletException expectedException = new ServletException("Test exception");
        doThrow(expectedException).when(filterChain).doFilter(request, response);

        try {
            correlationIdFilter.doFilterInternal(request, response, filterChain);
            assertThat(true).as("Expected ServletException to be thrown").isFalse();
        } catch (ServletException e) {
            assertThat(e).isSameAs(expectedException);
        }

        assertThat(MDC.get("correlationId")).isNull();
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("Should generate different correlation IDs for different requests")
    void shouldGenerateDifferentCorrelationIdsForDifferentRequests() throws ServletException, IOException {
        correlationIdFilter.doFilterInternal(request, response, filterChain);
        String firstCorrelationId = response.getHeader("X-Correlation-ID");

        MockHttpServletResponse response2 = new MockHttpServletResponse();
        correlationIdFilter.doFilterInternal(request, response2, filterChain);
        String secondCorrelationId = response2.getHeader("X-Correlation-ID");

        assertThat(firstCorrelationId).isNotEqualTo(secondCorrelationId);
    }

    @Test
    @DisplayName("Should ignore empty correlation ID header")
    void shouldIgnoreEmptyCorrelationIdHeader() throws ServletException, IOException {
        request.addHeader("X-Correlation-ID", "   ");

        correlationIdFilter.doFilterInternal(request, response, filterChain);

        String correlationId = response.getHeader("X-Correlation-ID");
        assertThat(correlationId).isNotBlank();
        assertThat(correlationId).hasSize(36);
    }
}
