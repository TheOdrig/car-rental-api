package com.akif.shared.config;

import com.akif.shared.web.PageableGuardInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final int maxPageSize;

    public WebConfig(@Value("${spring.data.web.pageable.max-page-size:100}") int maxPageSize) {
        this.maxPageSize = maxPageSize;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new PageableGuardInterceptor(maxPageSize)).addPathPatterns("/api/v1/**");
    }
}
