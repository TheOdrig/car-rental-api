package com.akif.shared.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Secret redaction in record toString")
class SecretRedactionTest {

    private static final Pattern SENSITIVE_NAME = Pattern.compile("(?i)(password|secret|token)$");

    @Test
    @DisplayName("no record leaks a password, token or secret component through toString")
    void recordsDoNotLeakSecrets() throws Exception {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false) {
            @Override
            protected boolean isCandidateComponent(
                    org.springframework.beans.factory.annotation.AnnotatedBeanDefinition definition) {
                return true;
            }
        };
        scanner.addIncludeFilter(new AssignableTypeFilter(Record.class));

        List<String> scanned = new ArrayList<>();
        List<String> leaks = new ArrayList<>();
        for (BeanDefinition definition : scanner.findCandidateComponents("com.akif")) {
            Class<?> type = Class.forName(definition.getBeanClassName());
            if (!type.isRecord()) {
                continue;
            }
            RecordComponent[] components = type.getRecordComponents();
            boolean sensitive = false;
            for (RecordComponent component : components) {
                sensitive |= component.getType() == String.class && SENSITIVE_NAME.matcher(component.getName()).find();
            }
            if (!sensitive) {
                continue;
            }
            scanned.add(type.getName());

            Class<?>[] parameterTypes = new Class<?>[components.length];
            Object[] arguments = new Object[components.length];
            List<String> secrets = new ArrayList<>();
            for (int i = 0; i < components.length; i++) {
                parameterTypes[i] = components[i].getType();
                if (components[i].getType() == String.class
                        && SENSITIVE_NAME.matcher(components[i].getName()).find()) {
                    String secret = "S3CR3T-" + components[i].getName() + "-9f3a";
                    secrets.add(secret);
                    arguments[i] = secret;
                } else if (components[i].getType() == long.class) {
                    arguments[i] = 1L;
                } else if (components[i].getType() == int.class) {
                    arguments[i] = 1;
                } else if (components[i].getType() == boolean.class) {
                    arguments[i] = false;
                }
            }
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            String rendered = constructor.newInstance(arguments).toString();
            for (String secret : secrets) {
                if (rendered.contains(secret)) {
                    leaks.add(type.getName() + " leaks " + secret + ": " + rendered);
                }
            }
        }

        assertThat(scanned).isNotEmpty();
        assertThat(leaks).isEmpty();
    }
}
