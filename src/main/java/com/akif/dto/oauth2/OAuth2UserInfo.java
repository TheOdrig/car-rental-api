package com.akif.dto.oauth2;

public record OAuth2UserInfo(
        String providerId,
        String email,
        String name,
        String avatarUrl,
        String provider
) {
}
