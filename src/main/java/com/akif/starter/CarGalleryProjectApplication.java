package com.akif.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@ComponentScan(basePackages = {"com.akif"})
@EnableJpaRepositories(basePackages = {"com.akif"})
@EntityScan(basePackages = {"com.akif"})
@EnableJpaAuditing
@EnableCaching
@SpringBootApplication
public class CarGalleryProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarGalleryProjectApplication.class, args);
    }

}
