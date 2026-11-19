package com.akif.auth.api;

public record UserProfileUpdatedEvent(Long userId, String displayName) {
}
