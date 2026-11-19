package com.akif;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Non-Java sources carry no comments")
class NoConfigCommentsTest {

    @Test
    @DisplayName("SQL migrations and seeds have no line or block comments")
    void sqlHasNoComments() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path file : files(Path.of("src"), ".sql")) {
            scanSql(file, Files.readString(file), offences);
        }
        assertThat(offences).isEmpty();
    }

    @Test
    @DisplayName("properties files have no comment lines")
    void propertiesHaveNoComments() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path file : files(Path.of("src"), ".properties")) {
            scanLines(file, Files.readAllLines(file), offences, "#", "!");
        }
        assertThat(offences).isEmpty();
    }

    @Test
    @DisplayName("shell and PowerShell scripts, hooks, Dockerfile and compose files have no comment lines")
    void scriptsHaveNoComments() throws IOException {
        List<String> offences = new ArrayList<>();
        List<Path> candidates = new ArrayList<>();
        candidates.addAll(files(Path.of("scripts"), ""));
        candidates.addAll(files(Path.of(".githooks"), ""));
        candidates.add(Path.of("Dockerfile"));
        candidates.add(Path.of("docker-compose.yml"));
        for (Path file : candidates) {
            List<String> lines = new ArrayList<>(Files.readAllLines(file));
            if (!lines.isEmpty() && lines.get(0).startsWith("#!")) {
                lines.set(0, "");
            }
            if (file.toString().endsWith(".ps1")) {
                scanLines(file, lines, offences, "#", "<#");
            } else {
                scanLines(file, lines, offences, "#");
            }
        }
        assertThat(offences).isEmpty();
    }

    @Test
    @DisplayName("XML and HTML resources have no comment blocks")
    void markupHasNoComments() throws IOException {
        List<String> offences = new ArrayList<>();
        for (Path file : files(Path.of("src"), ".xml", ".html")) {
            if (Files.readString(file).contains("<!--")) {
                offences.add(file.toString());
            }
        }
        assertThat(offences).isEmpty();
    }

    @Test
    @DisplayName("the SQL scanner ignores comment markers inside string literals")
    void sqlScannerIgnoresLiterals() {
        List<String> offences = new ArrayList<>();
        scanSql(Path.of("a.sql"), "SELECT '--x', '/* y */' FROM t;\n", offences);
        assertThat(offences).isEmpty();
        scanSql(Path.of("a.sql"), "SELECT 1; -- note\n", offences);
        scanSql(Path.of("a.sql"), "/* note */ SELECT 1;\n", offences);
        assertThat(offences).hasSize(2);
    }

    private static void scanLines(Path file, List<String> lines, List<String> offences, String... markers) {
        for (int i = 0; i < lines.size(); i++) {
            String trimmed = lines.get(i).stripLeading();
            for (String marker : markers) {
                if (trimmed.startsWith(marker)) {
                    offences.add(file + ":" + (i + 1));
                    break;
                }
            }
        }
    }

    private static void scanSql(Path file, String source, List<String> offences) {
        int line = 1;
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '\n') {
                line++;
                i++;
            } else if (c == '\'') {
                i++;
                while (i < n && source.charAt(i) != '\'') {
                    if (source.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                i++;
            } else if (source.startsWith("--", i) || source.startsWith("/*", i)) {
                offences.add(file + ":" + line);
                if (source.startsWith("--", i)) {
                    while (i < n && source.charAt(i) != '\n') {
                        i++;
                    }
                } else {
                    int end = source.indexOf("*/", i + 2);
                    int stop = end < 0 ? n : end + 2;
                    for (int k = i; k < stop; k++) {
                        if (source.charAt(k) == '\n') {
                            line++;
                        }
                    }
                    i = stop;
                }
            } else {
                i++;
            }
        }
    }

    private static List<Path> files(Path root, String... suffixes) throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> {
                        String name = path.toString();
                        if (suffixes.length == 1 && suffixes[0].isEmpty()) {
                            return true;
                        }
                        for (String suffix : suffixes) {
                            if (name.endsWith(suffix)) {
                                return true;
                            }
                        }
                        return false;
                    })
                    .sorted()
                    .toList();
        }
    }
}
