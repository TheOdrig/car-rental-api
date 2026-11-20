package com.akif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EntityScan(basePackages = { "com.akif", "org.springframework.modulith.events.jpa.updating" })
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@ConfigurationPropertiesScan
@SpringBootApplication
public class CarRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarRentalApplication.class, args);
    }

}
