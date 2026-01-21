package com.akif.auth.api;

public interface PasswordResetService {

    void requestPasswordReset(String email);

    void resetPassword(String token, String newPassword);

    boolean validateToken(String token);
}
