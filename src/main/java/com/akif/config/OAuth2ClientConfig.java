package com.akif.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OAuth2ClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 10000;

    @Bean
    public RestClient oAuth2RestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(CONNECT_TIMEOUT_MS));
        factory.setReadTimeout(Duration.ofMillis(READ_TIMEOUT_MS));

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
