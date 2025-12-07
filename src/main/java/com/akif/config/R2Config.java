package com.akif.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "r2")
@Getter
@Setter
public class R2Config {

    private String endpoint;
    private String bucketName;
    private String accessKeyId;
    private String secretAccessKey;

    public boolean isConfigured() {
        return endpoint != null && !endpoint.isBlank()
                && bucketName != null && !bucketName.isBlank()
                && accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank();
    }
}
