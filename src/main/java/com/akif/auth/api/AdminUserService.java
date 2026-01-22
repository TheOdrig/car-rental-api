package com.akif.auth.api;

import java.util.List;

public interface AdminUserService {

    AdminUserDetailResponse getUserDetailForAdmin(Long userId);

    void banUser(Long userId, String reason, Long adminId);

    void unbanUser(Long userId, String note, Long adminId);

    AdminNoteDto addAdminNote(Long userId, String text, Long adminId, String adminUsername);

    List<AdminNoteDto> getAdminNotes(Long userId);
}
