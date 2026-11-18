package com.akif.shared.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    private MockHttpServletRequest request(String remote, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.setRemoteAddr(remote);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    private ClientIpResolver resolver(boolean trust, int hops) {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustForwardedHeaders(trust);
        properties.setTrustedProxyHops(hops);
        return new ClientIpResolver(properties);
    }

    @Test
    @DisplayName("ignores forwarded headers unless they are explicitly trusted")
    void ignoresForwardedHeadersByDefault() {
        assertThat(resolver(false, 1).resolve(request("10.0.0.5", "203.0.113.7"))).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("uses the entry appended by the trusted proxy")
    void usesTrustedProxyEntry() {
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("a spoofed leading entry cannot change the resolved client")
    void spoofedLeadingEntryIgnored() {
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "9.9.9.9, 203.0.113.7")))
                .isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("skips as many entries from the right as there are trusted hops")
    void honoursConfiguredHops() {
        assertThat(resolver(true, 2).resolve(request("10.0.0.5", "203.0.113.7, 172.16.0.1")))
                .isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("falls back to the socket address when the header is missing or too short")
    void fallsBackWhenHeaderUnusable() {
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", null))).isEqualTo("10.0.0.5");
        assertThat(resolver(true, 2).resolve(request("10.0.0.5", "203.0.113.7"))).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("falls back to the socket address when the entry is not an IP literal")
    void fallsBackOnGarbage() {
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "not-an-ip"))).isEqualTo("10.0.0.5");
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "999.1.1.1"))).isEqualTo("10.0.0.5");
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "evil.example.com"))).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("accepts IPv6 literals")
    void acceptsIpv6() {
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "2001:db8::1"))).isEqualTo("2001:db8::1");
    }

    @Test
    @DisplayName("joins every X-Forwarded-For header line before counting hops")
    void joinsMultipleHeaderLines() {
        MockHttpServletRequest request = request("10.0.0.5", "9.9.9.9");
        request.addHeader("X-Forwarded-For", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "172.16.0.1");

        assertThat(resolver(true, 2).resolve(request)).isEqualTo("203.0.113.7");
        assertThat(resolver(true, 1).resolve(request)).isEqualTo("172.16.0.1");
    }

    @Test
    @DisplayName("prefers the trusted client IP header only when trust is enabled")
    void usesTrustedClientIpHeader() {
        MockHttpServletRequest request = request("10.0.0.5", "203.0.113.7, 172.68.1.1");
        request.addHeader("CF-Connecting-IP", "198.51.100.23");

        RateLimitProperties trusted = new RateLimitProperties();
        trusted.setTrustForwardedHeaders(true);
        trusted.setTrustedClientIpHeader("CF-Connecting-IP");
        RateLimitProperties untrusted = new RateLimitProperties();
        untrusted.setTrustForwardedHeaders(false);
        untrusted.setTrustedClientIpHeader("CF-Connecting-IP");

        assertThat(new ClientIpResolver(trusted).resolve(request)).isEqualTo("198.51.100.23");
        assertThat(new ClientIpResolver(untrusted).resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("falls back to X-Forwarded-For when the trusted client IP header is absent or not an IP")
    void trustedHeaderFallsBack() {
        RateLimitProperties properties = new RateLimitProperties();
        properties.setTrustForwardedHeaders(true);
        properties.setTrustedClientIpHeader("CF-Connecting-IP");
        ClientIpResolver resolver = new ClientIpResolver(properties);

        assertThat(resolver.resolve(request("10.0.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");

        MockHttpServletRequest garbage = request("10.0.0.5", "203.0.113.7");
        garbage.addHeader("CF-Connecting-IP", "not-an-ip");
        assertThat(resolver.resolve(garbage)).isEqualTo("203.0.113.7");
    }
}
