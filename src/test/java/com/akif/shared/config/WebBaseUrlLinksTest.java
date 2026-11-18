package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Web base URL links")
class WebBaseUrlLinksTest {

    private PropertySourcesPropertyResolver resolve(String profile, Map<String, Object> env) throws IOException {
        Map<String, Object> file = new HashMap<>();
        PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-" + profile + ".properties"))
                .forEach((k, v) -> file.put(k.toString(), v));
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new MapPropertySource("env", env));
        sources.addLast(new MapPropertySource("file", file));
        return new PropertySourcesPropertyResolver(sources);
    }

    @Test
    @DisplayName("prod OAuth2 redirect URIs target the unversioned web BFF callback route")
    void prodRedirectUris() throws IOException {
        PropertySourcesPropertyResolver resolver = resolve("prod", Map.of("APP_BASE_URL", "https://rent.example.com"));

        assertThat(resolver.getProperty("oauth2.google.redirect-uri"))
                .isEqualTo("https://rent.example.com/api/auth/callback/google");
        assertThat(resolver.getProperty("oauth2.github.redirect-uri"))
                .isEqualTo("https://rent.example.com/api/auth/callback/github");
        assertThat(resolver.getProperty("app.password-reset.base-url")).isEqualTo("https://rent.example.com");
    }

    @Test
    @DisplayName("dev defaults point at the web dev server, not the API port")
    void devDefaults() throws IOException {
        PropertySourcesPropertyResolver resolver = resolve("dev", Map.of());

        assertThat(resolver.getProperty("oauth2.google.redirect-uri"))
                .isEqualTo("http://localhost:3000/api/auth/callback/google");
        assertThat(resolver.getProperty("oauth2.github.redirect-uri"))
                .isEqualTo("http://localhost:3000/api/auth/callback/github");
        assertThat(resolver.getProperty("app.password-reset.base-url")).isEqualTo("http://localhost:3000");
    }
}
