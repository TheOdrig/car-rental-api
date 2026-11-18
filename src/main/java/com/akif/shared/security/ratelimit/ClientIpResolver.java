package com.akif.shared.security.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String UNKNOWN_CLIENT = "unknown";
    private static final Pattern IPV4 = Pattern.compile("^(\\d{1,3})(\\.\\d{1,3}){3}$");

    private final RateLimitProperties properties;

    public ClientIpResolver(RateLimitProperties properties) {
        this.properties = properties;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddress = normalize(request.getRemoteAddr());
        if (!properties.isTrustForwardedHeaders()) {
            return remoteAddress;
        }

        String header = request.getHeader(FORWARDED_FOR_HEADER);
        if (header == null || header.isBlank()) {
            return remoteAddress;
        }

        String[] entries = header.split(",");
        int index = entries.length - Math.max(1, properties.getTrustedProxyHops());
        if (index < 0) {
            return remoteAddress;
        }

        String candidate = entries[index].trim();
        return isIpLiteral(candidate) ? normalize(candidate) : remoteAddress;
    }

    private static String normalize(String address) {
        return address == null || address.isBlank() ? UNKNOWN_CLIENT : address.trim().toLowerCase();
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
