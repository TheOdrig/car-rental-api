package com.akif.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})(\\.\\d{1,3}){3}$");

    private final RateLimitProperties properties;
    private final List<IpAddressMatcher> trustedProxies;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
        this.trustedProxies = properties.getTrustedProxyCidrs().stream()
                .map(String::trim)
                .filter(cidr -> !cidr.isEmpty())
                .map(IpAddressMatcher::new)
                .toList();
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!isTrustedProxy(request.getRemoteAddr())) {
            return remoteAddress;
        }

        String trustedHeader = properties.getTrustedClientIpHeader();
        if (trustedHeader != null && !trustedHeader.isBlank()) {
            String value = request.getHeader(trustedHeader.trim());
            if (value != null && isIpLiteral(value.trim())) {
                return normalize(value);
            }
        }

        if (!properties.isTrustForwardedHeaders()) {
            return remoteAddress;
        }

        List<String> entries = forwardedEntries(request);
        int index = entries.size() - Math.max(1, properties.getTrustedProxyHops());
        if (index < 0) {
            return remoteAddress;
        }

        String candidate = entries.get(index);
        return isIpLiteral(candidate) ? normalize(candidate) : remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        String address = remoteAddress.trim();
        for (IpAddressMatcher matcher : trustedProxies) {
            try {
                if (matcher.matches(address)) {
                    return true;
                }
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return false;
    }

    private static List<String> forwardedEntries(HttpServletRequest request) {
        List<String> entries = new ArrayList<>();
        Enumeration<String> headers = request.getHeaders(FORWARDED_FOR_HEADER);
        while (headers != null && headers.hasMoreElements()) {
            String header = headers.nextElement();
            if (header == null) {
                continue;
            }
            for (String entry : header.split(",", -1)) {
                entries.add(entry.trim());
            }
        }
        return entries;
    }

    private static String normalize(String address) {
        if (address == null || address.isBlank()) {
            return UNKNOWN_CLIENT;
        }
        String trimmed = address.trim().toLowerCase();
        return trimmed.indexOf(':') < 0 ? trimmed : ipv6Prefix(trimmed);
    }

    private static String ipv6Prefix(String address) {
        try {
            InetAddress parsed = InetAddress.getByName(address);
            if (parsed instanceof Inet6Address) {
                byte[] bytes = parsed.getAddress();
                return String.format("%x:%x:%x:%x::/64",
                        ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff),
                        ((bytes[2] & 0xff) << 8) | (bytes[3] & 0xff),
                        ((bytes[4] & 0xff) << 8) | (bytes[5] & 0xff),
                        ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff));
            }
            return parsed.getHostAddress();
        } catch (UnknownHostException | RuntimeException e) {
            return UNKNOWN_CLIENT;
        }
    }

    private static boolean isIpLiteral(String value) {
        if (value.length() > 45) {
            return false;
        }
        if (IPV4.matcher(value).matches()) {
            for (String part : value.split("\\.")) {
                if (Integer.parseInt(part) > 255) {
                    return false;
                }
            }
            return true;
        }
        if (value.indexOf(':') < 0) {
            return false;
        }
        try {
            InetAddress.getByName(value);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
