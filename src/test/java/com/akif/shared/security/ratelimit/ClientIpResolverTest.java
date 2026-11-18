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
}
