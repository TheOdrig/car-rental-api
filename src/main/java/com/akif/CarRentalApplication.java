package com.akif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan(basePackages = { "com.akif" })
@EnableJpaRepositories(basePackages = { "com.akif" })
@EntityScan(basePackages = { "com.akif" })
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class CarRentalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarRentalApplication.class, args);
    }

}
