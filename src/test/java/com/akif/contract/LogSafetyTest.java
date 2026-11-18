package com.akif.contract;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Log safety")
class LogSafetyTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");
    private static final Pattern LOG_CALL = Pattern.compile("\\blog\\.(trace|debug|info|warn|error)\\(");
    private static final Pattern STRING_LITERAL = Pattern.compile("\"(?:[^\"\\\\]|\\\\.)*\"");
    private static final Pattern SENSITIVE = Pattern.compile(
            "(?i)(password|passwd|secret|token|authorization|cookie|cardnumber|card_number|cvc|cvv|iban|resetlink"
                    + "|reseturl|resetpath|apikey|api_key|bearer|credential|signature|clientsecret)");
    private static final Pattern HARMLESS_IDENTIFIER = Pattern.compile(
            "(?i)(tokenid|tokenversion|tokencount|tokenuse|tokentype|tokenuri|expir|hash|fingerprint|count|length"
                    + "|size|enabled|exists|isblank|isempty|isnull|resetid|passwordreset(requested)?event)");
    private static final Pattern FORBIDDEN_OUTPUT = Pattern.compile(
            "\\bSystem\\.(out|err)\\.|\\.printStackTrace\\(");

    @Test
    @DisplayName("log statements never pass tokens, passwords, secrets, card data or reset links")
    void logStatementsDoNotReferenceSensitiveValues() throws IOException {
        List<String> findings = new ArrayList<>();
        for (Path file : javaFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher call = LOG_CALL.matcher(source);
            while (call.find()) {
                String statement = statementFrom(source, call.end());
                String expression = STRING_LITERAL.matcher(statement).replaceAll("\"\"");
                Matcher sensitive = SENSITIVE.matcher(expression);
                while (sensitive.find()) {
                    String identifier = identifierAround(expression, sensitive.start(), sensitive.end());
                    if (!HARMLESS_IDENTIFIER.matcher(identifier).find()) {
                        findings.add("%s:%d logs %s".formatted(relative(file), lineOf(source, call.start()),
                                identifier));
                    }
                }
            }
        }
        assertThat(findings).as("sensitive values must not be logged").isEmpty();
    }

    @Test
    @DisplayName("production code does not write to the console or print stack traces")
    void noConsoleOutput() throws IOException {
        List<String> findings = new ArrayList<>();
        for (Path file : javaFiles()) {
            String source = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = FORBIDDEN_OUTPUT.matcher(source);
            while (matcher.find()) {
                findings.add("%s:%d".formatted(relative(file), lineOf(source, matcher.start())));
            }
        }
        assertThat(findings).isEmpty();
    }

    private static String relative(Path file) {
        return SOURCE_ROOT.relativize(file).toString().replace('\\', '/');
    }

    private static List<Path> javaFiles() throws IOException {
        try (Stream<Path> files = Files.walk(SOURCE_ROOT)) {
            return files.filter(path -> path.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String statementFrom(String source, int start) {
        int depth = 1;
        boolean inString = false;
        int index = start;
        while (index < source.length() && depth > 0) {
            char c = source.charAt(index);
            if (inString) {
                if (c == '\\') {
                    index++;
                } else if (c == '"') {
                    inString = false;
                }
            } else if (c == '"') {
                inString = true;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            }
            index++;
        }
        return source.substring(start, Math.min(index, source.length()));
    }

    private static String identifierAround(String text, int start, int end) {
        int from = start;
        while (from > 0 && Character.isJavaIdentifierPart(text.charAt(from - 1))) {
            from--;
        }
        int to = end;
        while (to < text.length() && Character.isJavaIdentifierPart(text.charAt(to))) {
            to++;
        }
        return text.substring(from, to);
    }

    private static int lineOf(String source, int index) {
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (source.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
