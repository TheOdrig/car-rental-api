package com.akif.auth.api;

public record AvatarUploadResponse(
        String avatarUrl,
        String message) {
    public static AvatarUploadResponse success(String avatarUrl) {
        return new AvatarUploadResponse(avatarUrl, "Avatar uploaded successfully");
    }

    public static AvatarUploadResponse deleted() {
        return new AvatarUploadResponse(null, "Avatar deleted successfully");
    }
}
