package com.akif.shared.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientIpResolver")
class ClientIpResolverTest {

    private MockHttpServletRequest request(String remote, String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr(remote);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }

    private ClientIpResolver resolver(boolean trust, int hops) {
        return new ClientIpResolver(new RateLimitProperties(true, trust, hops, "", List.of("10.0.0.0/8"), 100_000, null));
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
        assertThat(resolver(true, 1).resolve(request("10.0.0.5", "2001:db8::1"))).isEqualTo("2001:db8:0:0::/64");
    }

    @Test
    @DisplayName("buckets every IPv6 address of a /64 together and keeps other prefixes apart")
    void bucketsIpv6ByPrefix() {
        ClientIpResolver resolver = resolver(false, 1);
        String first = resolver.resolve(request("2001:db8:1:2:aaaa:bbbb:cccc:dddd", null));
        String second = resolver.resolve(request("2001:DB8:1:2:1::9", null));
        String other = resolver.resolve(request("2001:db8:1:3::1", null));

        assertThat(first).isEqualTo("2001:db8:1:2::/64").isEqualTo(second);
        assertThat(other).isNotEqualTo(first);
    }

    @Test
    @DisplayName("treats an IPv4-mapped IPv6 address as the IPv4 address")
    void mapsIpv4MappedAddresses() {
        assertThat(resolver(false, 1).resolve(request("::ffff:203.0.113.9", null))).isEqualTo("203.0.113.9");
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

    private RateLimitProperties headerProperties(boolean trustForwarded, List<String> cidrs) {
        return new RateLimitProperties(true, trustForwarded, 1, "X-Client-IP", cidrs, 100_000, null);
    }

    @Test
    @DisplayName("honours the trusted client IP header when the remote address is inside a trusted CIDR")
    void honoursHeaderFromTrustedProxy() {
        MockHttpServletRequest request = request("10.1.2.3", null);
        request.addHeader("X-Client-IP", "198.51.100.23");

        assertThat(new ClientIpResolver(headerProperties(false, List.of("10.0.0.0/8"))).resolve(request))
                .isEqualTo("198.51.100.23");
    }

    @Test
    @DisplayName("ignores the trusted client IP header from a remote address outside every trusted CIDR")
    void ignoresHeaderFromUntrustedRemote() {
        MockHttpServletRequest request = request("203.0.113.50", "9.9.9.9");
        request.addHeader("X-Client-IP", "198.51.100.23");

        assertThat(new ClientIpResolver(headerProperties(true, List.of("10.0.0.0/8"))).resolve(request))
                .isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("ignores every forwarded header when no trusted CIDR is configured")
    void emptyCidrListTrustsNobody() {
        MockHttpServletRequest request = request("10.0.0.5", "203.0.113.7");
        request.addHeader("X-Client-IP", "198.51.100.23");

        assertThat(new ClientIpResolver(headerProperties(true, List.of())).resolve(request)).isEqualTo("10.0.0.5");
    }

    @Test
    @DisplayName("ignores X-Forwarded-For from a remote address outside the trusted CIDRs")
    void ignoresForwardedForFromUntrustedRemote() {
        RateLimitProperties properties = new RateLimitProperties(true, true, 1, "", List.of("10.0.0.0/8"), 100_000, null);

        assertThat(new ClientIpResolver(properties).resolve(request("203.0.113.50", "9.9.9.9")))
                .isEqualTo("203.0.113.50");
    }

    @Test
    @DisplayName("supports IPv6 trusted ranges")
    void supportsIpv6Cidrs() {
        RateLimitProperties properties = headerProperties(false, List.of("fd00::/8"));
        MockHttpServletRequest trusted = request("fd00::5", null);
        trusted.addHeader("X-Client-IP", "198.51.100.23");
        MockHttpServletRequest untrusted = request("2001:db8::5", null);
        untrusted.addHeader("X-Client-IP", "198.51.100.23");

        ClientIpResolver resolver = new ClientIpResolver(properties);
        assertThat(resolver.resolve(trusted)).isEqualTo("198.51.100.23");
        assertThat(resolver.resolve(untrusted)).isEqualTo("2001:db8:0:0::/64");
    }

    @Test
    @DisplayName("falls back to X-Forwarded-For when the trusted client IP header is absent or not an IP")
    void trustedHeaderFallsBack() {
        ClientIpResolver resolver = new ClientIpResolver(headerProperties(true, List.of("10.0.0.0/8")));

        assertThat(resolver.resolve(request("10.0.0.5", "203.0.113.7"))).isEqualTo("203.0.113.7");

        MockHttpServletRequest garbage = request("10.0.0.5", "203.0.113.7");
        garbage.addHeader("X-Client-IP", "not-an-ip");
        assertThat(resolver.resolve(garbage)).isEqualTo("203.0.113.7");
    }
}
