package com.akif.shared.security;

import com.akif.shared.security.ratelimit.ClientIpResolver;
import com.akif.shared.security.ratelimit.RateLimitEndpointClassifier;
import com.akif.shared.security.ratelimit.RateLimitFilter;
import com.akif.shared.security.ratelimit.RateLimitProperties;
import com.akif.shared.security.ratelimit.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.context.SecurityContextHolderFilter;

import org.springframework.security.config.Customizer;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(RateLimitProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private static final List<String> TOKENLESS_PATHS = List.of("/api/v1/auth/**", "/api/v1/webhooks/**");

    private final CorrelationIdFilter correlationIdFilter;
    private final JwtUserAuthenticationConverter jwtUserAuthenticationConverter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitEndpointClassifier rateLimitClassifier;
    private final ClientIpResolver clientIpResolver;
    private final RateLimitService rateLimitService;
    private final ApiErrorWriter apiErrorWriter;
    private final RequestBodyLimitFilter requestBodyLimitFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain managementFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(PathPatternRequestMatcher.withDefaults().matcher("/actuator/**"))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(EndpointRequest.to("health", "info", "prometheus")).permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.NOT_FOUND))
                        .accessDeniedHandler((request, response, exception) ->
                                response.setStatus(HttpStatus.NOT_FOUND.value())));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Value("${springdoc.swagger-ui.enabled:true}") boolean swaggerUiEnabled)
            throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::deny);
                    headers.httpStrictTransportSecurity(hsts -> hsts
                            .requestMatcher(AnyRequestMatcher.INSTANCE)
                            .includeSubDomains(true)
                            .maxAgeInSeconds(31536000));
                    if (!swaggerUiEnabled) {
                        headers.contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'"));
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(ApiAccessPolicy.DOCUMENTATION_PATHS.toArray(String[]::new)).permitAll();
                    if (swaggerUiEnabled) {
                        auth.requestMatchers(ApiAccessPolicy.SWAGGER_UI_ENTRY).permitAll();
                    }
                    ApiAccessPolicy.RULES.forEach(rule -> apply(auth, rule));
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver())
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtUserAuthenticationConverter)))
                .addFilterBefore(correlationIdFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(requestBodyLimitFilter, CorrelationIdFilter.class)
                .addFilterAfter(new MdcUserFilter(), BearerTokenAuthenticationFilter.class)
                .addFilterBefore(new RateLimitFilter(rateLimitProperties, rateLimitClassifier, clientIpResolver,
                        rateLimitService, apiErrorWriter), BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    private static void apply(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            ApiAccessPolicy.Rule rule) {
        AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizedUrl url = rule.method() == null
                ? auth.requestMatchers(rule.pattern())
                : auth.requestMatchers(rule.method(), rule.pattern());
        switch (rule.access()) {
            case PUBLIC -> url.permitAll();
            case AUTHENTICATED -> url.authenticated();
            case USER -> url.hasRole("USER");
            case ADMIN -> url.hasRole("ADMIN");
        }
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
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestBodyLimitFilter> requestBodyLimitFilterRegistration(RequestBodyLimitFilter filter) {
        FilterRegistrationBean<RequestBodyLimitFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
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