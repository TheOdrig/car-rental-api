package com.akif.config;

import com.akif.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/health", "/actuator/health").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/oauth2/authorize/**").permitAll()
                .requestMatchers("/api/oauth2/callback/**").permitAll()
                .requestMatchers("/api/oauth2/link/**").authenticated()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/rentals/request").hasRole("USER")
                .requestMatchers(HttpMethod.POST, "/api/rentals/*/confirm").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/rentals/*/pickup").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/rentals/*/return").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/rentals/*/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/rentals/me").hasRole("USER")
                .requestMatchers(HttpMethod.GET, "/api/rentals/admin").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/rentals/*").authenticated()

                .requestMatchers("/api/cars/business/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/cars").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cars/{id:\\d+}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cars/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/cars/statistics/**").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/cars").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/cars/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/cars/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/cars/**").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/exchange-rates/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/exchange-rates/convert").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/exchange-rates/refresh").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/pricing/preview").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pricing/strategies").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/pricing/calculate").authenticated()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}