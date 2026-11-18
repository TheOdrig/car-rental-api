package com.akif.contract;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.fail;

final class ContractSnapshots {

    private ContractSnapshots() {
    }

    static String normalize(String text) {
        return text.replace("\r\n", "\n");
    }

    static void verify(Path path, String expected, String updateProperty, String updateCommand) throws IOException {
        if (Boolean.getBoolean(updateProperty)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, expected, StandardCharsets.UTF_8);
            return;
        }
        if (!Files.exists(path)) {
            fail("Missing contract snapshot %s. Generate it with: %s".formatted(path, updateCommand));
        }
        String actual = normalize(Files.readString(path, StandardCharsets.UTF_8));
        if (!actual.equals(expected)) {
            fail("Contract drift in %s. Review the change and regenerate it with: %s".formatted(path, updateCommand));
        }
    }
}
