package com.akif.auth.api;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {

    Page<AdminUserListItem> getAllUsers(String role, String status, String search, Pageable pageable);

    AdminUserDetailResponse getUserDetailForAdmin(Long userId);

    void banUser(Long userId, String reason, Long adminId);

    void unbanUser(Long userId, String note, Long adminId);

    AdminNoteDto addAdminNote(Long userId, String text, Long adminId, String adminUsername);

    List<AdminNoteDto> getAdminNotes(Long userId);

    UserStatsResponse getStats();
}