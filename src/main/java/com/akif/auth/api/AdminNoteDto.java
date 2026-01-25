package com.akif.auth.api;

import java.time.LocalDateTime;

public record AdminNoteDto(
        Long id,
        Long userId,
        Long adminId,
        String adminUsername,
        String text,
        LocalDateTime createdAt) {
}
