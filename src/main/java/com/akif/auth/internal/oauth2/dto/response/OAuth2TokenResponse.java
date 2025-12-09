package com.akif.auth.internal.oauth2.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuth2TokenResponse(

    @JsonProperty("access_token")
    String accessToken,

    @JsonProperty("token_type")
    String tokenType,

    @JsonProperty("expires_in")
    Long expiresIn,

    @JsonProperty("refresh_token")
    String refreshToken,

    @JsonProperty("scope")
    String scope,

    @JsonProperty("id_token")
    String idToken
) {}