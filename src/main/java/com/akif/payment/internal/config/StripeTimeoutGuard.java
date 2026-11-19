package com.akif.payment.internal.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class StripeTimeoutGuard {

    private static final Pattern DURATION = Pattern.compile("^ *([0-9]+) *(us|ms|s|min|h|d)? *$");

    private final StripeConfig stripeConfig;

    @Value("${DB_IDLE_IN_TRANSACTION_TIMEOUT:60s}")
    private String idleInTransactionTimeout;

    @PostConstruct
    void verify() {
        check(stripeConfig.getConnectTimeoutMs(), stripeConfig.getReadTimeoutMs(),
                stripeConfig.getMaxNetworkRetries(), idleInTransactionTimeout);
    }

    static void check(long connectTimeoutMs, long readTimeoutMs, int retries, String idleTimeout) {
        long idleMs = toMillis(idleTimeout);
        if (idleMs <= 0) {
            return;
        }
        long worstCaseMs = (connectTimeoutMs + readTimeoutMs) * (retries + 1L);
        if (worstCaseMs >= idleMs) {
            throw new IllegalStateException("Stripe worst-case call time of " + worstCaseMs
                    + " ms ((connect + read timeout) x (retries + 1)) must stay below the database "
                    + "idle_in_transaction_session_timeout of " + idleMs
                    + " ms, otherwise transactions that hold locks across Stripe calls are killed mid-flight");
        }
    }

    static long toMillis(String value) {
        Matcher matcher = DURATION.matcher(value == null ? "" : value.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw new IllegalStateException("DB_IDLE_IN_TRANSACTION_TIMEOUT is not a PostgreSQL duration: " + value);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "ms" : matcher.group(2);
        return switch (unit) {
            case "us" -> amount / 1000;
            case "s" -> amount * 1000;
            case "min" -> amount * 60_000;
            case "h" -> amount * 3_600_000;
            case "d" -> amount * 86_400_000;
            default -> amount;
        };
    }
}
