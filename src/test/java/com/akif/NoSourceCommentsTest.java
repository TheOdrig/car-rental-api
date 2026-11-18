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

@DisplayName("Java sources carry no comments")
class NoSourceCommentsTest {

    private static final char BACKSLASH = (char) 92;

    @Test
    @DisplayName("no line, block or Javadoc comment exists under src")
    void noComments() throws IOException {
        List<String> offences = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                scan(file, Files.readString(file), offences);
            }
        }
        assertThat(offences).isEmpty();
    }

    @Test
    @DisplayName("the scanner ignores comment markers inside string, char and text block literals")
    void scannerIgnoresLiterals() {
        List<String> offences = new ArrayList<>();
        scan(Path.of("Sample.java"), "String a = \"//x\"; char c = '/'; String b = \"\"\"\n/* y */\n\"\"\";\n", offences);
        assertThat(offences).isEmpty();
        scan(Path.of("Sample.java"), "int a = 1; // note\n", offences);
        scan(Path.of("Sample.java"), "/** doc */ class A {}\n", offences);
        assertThat(offences).hasSize(2);
    }

    private static void scan(Path file, String source, List<String> offences) {
        int line = 1;
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            if (c == '\n') {
                line++;
                i++;
            } else if (source.startsWith("\"\"\"", i)) {
                i += 3;
                while (i < n && !source.startsWith("\"\"\"", i)) {
                    if (source.charAt(i) == BACKSLASH) {
                        i++;
                    } else if (source.charAt(i) == '\n') {
                        line++;
                    }
                    i++;
                }
                i += 3;
            } else if (c == '"' || c == '\'') {
                i++;
                while (i < n && source.charAt(i) != c && source.charAt(i) != '\n') {
                    if (source.charAt(i) == BACKSLASH) {
                        i++;
                    }
                    i++;
                }
                i++;
            } else if (source.startsWith("//", i) || source.startsWith("/*", i)) {
                offences.add(file + ":" + line);
                if (source.startsWith("//", i)) {
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
}
