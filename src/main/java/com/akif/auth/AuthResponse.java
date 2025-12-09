package com.akif.auth;

public record AuthResponse(

    String accessToken,
    String refreshToken,
    String tokenType,
    Long expiresIn,
    String username
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private String username;

        public Builder accessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder refreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder tokenType(String tokenType) {
            this.tokenType = tokenType;
            return this;
        }

        public Builder expiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public AuthResponse build() {
            return new AuthResponse(accessToken, refreshToken, tokenType, expiresIn, username);
        }
    }
}
