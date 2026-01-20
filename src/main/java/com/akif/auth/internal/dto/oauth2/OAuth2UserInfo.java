package com.akif.auth.internal.dto.oauth2;

public record OAuth2UserInfo(
        String providerId,
        String email,
        String name,
        String firstName,
        String lastName,
        String avatarUrl,
        String provider) {
}
