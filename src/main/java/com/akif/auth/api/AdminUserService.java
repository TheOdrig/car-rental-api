package com.akif.auth.api;

public interface AdminUserService {

    AdminUserDetailResponse getUserDetailForAdmin(Long userId);

    void banUser(Long userId, String reason, Long adminId);

    void unbanUser(Long userId, String note, Long adminId);
}
