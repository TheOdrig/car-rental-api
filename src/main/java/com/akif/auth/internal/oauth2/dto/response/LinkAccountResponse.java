package com.akif.auth.internal.oauth2.dto.response;

public record LinkAccountResponse(

    String message,
    String provider,
    String providerEmail,
    String linkedAt
) {}
