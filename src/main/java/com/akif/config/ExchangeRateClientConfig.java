package com.akif.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ExchangeRateClientConfig {

    @Value("${exchange-rate.api.base-url}")
    private String baseUrl;

    @Getter
    @Value("${exchange-rate.api.key}")
    private String apiKey;

    @Value("${exchange-rate.api.connect-timeout}")
    private int connectTimeout;

    @Value("${exchange-rate.api.read-timeout}")
    private int readTimeout;

    @Bean
    public RestClient exchangeRateRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));
        
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }
}
