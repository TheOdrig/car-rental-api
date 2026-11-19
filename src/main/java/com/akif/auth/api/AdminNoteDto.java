package com.akif.auth.api;

import java.time.Instant;

public record AdminNoteDto(
        Long id,
        Long userId,
        Long adminId,
        String adminUsername,
        String text,
        Instant createdAt) {
}
