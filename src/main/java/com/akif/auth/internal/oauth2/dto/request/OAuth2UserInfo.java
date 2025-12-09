package com.akif.auth.internal.oauth2.dto.request;

public record OAuth2UserInfo(
        String providerId,
        String email,
        String name,
        String avatarUrl,
        String provider
) {
}
