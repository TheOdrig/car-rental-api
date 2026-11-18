package com.akif.payment.internal.config;

import com.stripe.net.RequestOptions;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.webhook.tolerance-seconds:300}")
    private long webhookToleranceSeconds;

    @Value("${stripe.publishable-key:}")
    private String publishableKey;

    @Value("${stripe.webhook.max-body-bytes:262144}")
    private long webhookMaxBodyBytes;

    @Value("${stripe.metadata.environment:}")
    private String metadataEnvironment;

    @Value("${stripe.api.base-url:https://api.stripe.com}")
    private String apiBaseUrl;

    @Value("${stripe.api.max-network-retries:1}")
    private int maxNetworkRetries;

    @Value("${stripe.api.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${stripe.api.read-timeout-ms:10000}")
    private int readTimeoutMs;

    public RequestOptions.RequestOptionsBuilder requestOptions() {
        return RequestOptions.builder()
                .setApiKey(apiKey)
                .setBaseUrl(apiBaseUrl)
                .setConnectTimeout(connectTimeoutMs)
                .setReadTimeout(readTimeoutMs)
                .setMaxNetworkRetries(maxNetworkRetries);
    }
}
