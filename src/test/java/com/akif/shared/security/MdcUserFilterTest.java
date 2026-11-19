package com.akif.shared.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("MdcUserFilter")
class MdcUserFilterTest {

    private final MdcUserFilter filter = new MdcUserFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("exposes the authenticated user while the request runs and clears it afterwards")
    void exposesAuthenticatedUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                "alice", "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")));
        AtomicReference<String> seen = new AtomicReference<>();
        FilterChain chain = (request, response) -> seen.set(MDC.get("userId"));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isEqualTo("alice");
        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    @DisplayName("leaves the user empty for anonymous callers")
    void ignoresAnonymousCallers() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));
        AtomicReference<String> seen = new AtomicReference<>("unset");
        FilterChain chain = (request, response) -> seen.set(MDC.get("userId"));

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        assertThat(seen.get()).isNull();
    }
}
