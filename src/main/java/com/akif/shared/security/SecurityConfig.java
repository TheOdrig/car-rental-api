package com.akif.shared.security;

import com.akif.shared.security.ratelimit.ClientIpResolver;
import com.akif.shared.security.ratelimit.RateLimitEndpointClassifier;
import com.akif.shared.security.ratelimit.RateLimitFilter;
import com.akif.shared.security.ratelimit.RateLimitProperties;
import com.akif.shared.security.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
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
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> TOKENLESS_PATHS = List.of("/api/auth/**", "/api/webhooks/**");

    private final CorrelationIdFilter correlationIdFilter;
    private final JwtUserAuthenticationConverter jwtUserAuthenticationConverter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitEndpointClassifier rateLimitClassifier;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService rateLimitService;
    private final ApiErrorWriter apiErrorWriter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/health", "/actuator/health").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/oauth2/authorize/**").permitAll()
                        .requestMatchers("/api/oauth2/callback/**").permitAll()
                        .requestMatchers("/api/oauth2/link/**").authenticated()
                        .requestMatchers("/api/files/**").permitAll()
                        .requestMatchers("/api/webhooks/**").permitAll()
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
                        .requestMatchers(HttpMethod.GET, "/api/cars/{id:\\d+}/availability/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/cars/availability/search").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cars/{id:\\d+}/similar").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cars/active").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cars/filter-options").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/cars/search").permitAll()

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

                        .requestMatchers("/api/admin/late-returns/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/rentals/*/penalty/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/quick-actions/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/dashboard/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/alerts/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUserAuthenticationConverter)))
                .addFilterBefore(correlationIdFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(new RateLimitFilter(rateLimitProperties, rateLimitClassifier, clientIpResolver,
                        rateLimitService, apiErrorWriter), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> tokenless = TOKENLESS_PATHS.stream()
                .<RequestMatcher>map(PathPatternRequestMatcher.withDefaults()::matcher)
                .toList();
        OrRequestMatcher tokenlessMatcher = new OrRequestMatcher(tokenless);
        return request -> tokenlessMatcher.matches(request) ? null : resolver.resolve(request);
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