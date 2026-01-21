package com.akif.auth.api;

public interface PasswordService {

    void changePassword(String username, String currentPassword, String newPassword);

    boolean verifyPassword(String rawPassword, String encodedPassword);

    String hashPassword(String rawPassword);
}
