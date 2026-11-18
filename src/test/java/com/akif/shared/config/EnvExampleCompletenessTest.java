package com.akif.shared.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName(".env.example completeness")
class EnvExampleCompletenessTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("[$][{]([A-Z][A-Z0-9_]*)[:}]");
    private static final Pattern ASSIGNMENT = Pattern.compile("^#?[ ]*([A-Z][A-Z0-9_]*)=", Pattern.MULTILINE);

    private Set<String> placeholders(Path file) throws IOException {
        Set<String> names = new TreeSet<>();
        Matcher matcher = PLACEHOLDER.matcher(Files.readString(file));
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private Set<String> listed() throws IOException {
        Set<String> names = new TreeSet<>();
        Matcher matcher = ASSIGNMENT.matcher(Files.readString(Path.of(".env.example")));
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    @Test
    @DisplayName("lists every environment variable the prod profile references")
    void listsEveryProdVariable() throws IOException {
        Set<String> referenced = placeholders(Path.of("src/main/resources/application-prod.properties"));

        assertThat(referenced).isNotEmpty();
        assertThat(listed()).containsAll(referenced);
    }

    @Test
    @DisplayName("lists every environment variable the base configuration references")
    void listsEveryBaseVariable() throws IOException {
        Set<String> referenced = placeholders(Path.of("src/main/resources/application.properties"));

        assertThat(listed()).containsAll(referenced);
    }

    @Test
    @DisplayName("keeps secrets out of the example values")
    void holdsOnlyPlaceholders() throws IOException {
        assertThat(Files.readString(Path.of(".env.example"))).doesNotContain("sk_live").doesNotContain("pk_live");
    }
}
