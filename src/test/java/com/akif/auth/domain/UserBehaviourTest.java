package com.akif.auth.domain;

import com.akif.auth.domain.enums.AuthProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("User behaviour")
class UserBehaviourTest {

    private final LocalDateTime now = LocalDateTime.of(2026, 11, 18, 9, 0);

    private User activeUser() {
        return User.builder().username("jane").email("jane@example.com").enabled(true).build();
    }

    @Test
    @DisplayName("ban flags the account, records who banned it and disables sign-in")
    void banRecordsAuditAndDisablesUser() {
        User user = activeUser();

        user.ban("fraud", 7L, now);

        assertThat(user.getIsBanned()).isTrue();
        assertThat(user.getBannedAt()).isEqualTo(now);
        assertThat(user.getBannedBy()).isEqualTo(7L);
        assertThat(user.getBanReason()).isEqualTo("fraud");
        assertThat(user.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("unban lifts the ban and re-enables the account")
    void unbanReenablesUser() {
        User user = activeUser();
        user.ban("fraud", 7L, now);

        user.unban(now.plusDays(2));

        assertThat(user.getIsBanned()).isFalse();
        assertThat(user.getUnbannedAt()).isEqualTo(now.plusDays(2));
        assertThat(user.getEnabled()).isTrue();
    }

    @Test
    @DisplayName("updateProfile stores the names and clears a blank phone number")
    void updateProfileClearsBlankPhone() {
        User user = activeUser();

        user.updateProfile("Jane", "Doe", "+905551112233");
        assertThat(user.getPhone()).isEqualTo("+905551112233");

        user.updateProfile("Jane", "Doe", " ");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Doe");
    }

    @Test
    @DisplayName("avatar can be changed and removed, and a social login switches the provider to MIXED")
    void avatarAndSocialLoginOperations() {
        User user = activeUser();

        user.changeAvatar("avatars/jane.png");
        assertThat(user.getAvatarUrl()).isEqualTo("avatars/jane.png");
        user.removeAvatar();
        assertThat(user.getAvatarUrl()).isNull();

        user.linkSocialLogin();
        assertThat(user.getAuthProvider()).isEqualTo(AuthProvider.MIXED);
    }

    @Test
    @DisplayName("changePassword replaces the stored hash")
    void changePasswordReplacesHash() {
        User user = activeUser();

        user.changePassword("hash-2");

        assertThat(user.getPassword()).isEqualTo("hash-2");
    }

    @Test
    @DisplayName("an enabled, unbanned, undeleted user is usable")
    void activeUserIsUsable() {
        assertThat(activeUser().isUsable()).isTrue();
    }

    @Test
    @DisplayName("a banned, disabled or soft-deleted user is not usable")
    void blockedUsersAreNotUsable() {
        User banned = activeUser();
        banned.ban("fraud", 7L, now);
        User disabled = User.builder().username("d").email("d@example.com").enabled(false).build();
        User deleted = activeUser();
        deleted.softDelete();

        assertThat(banned.isUsable()).isFalse();
        assertThat(disabled.isUsable()).isFalse();
        assertThat(deleted.isUsable()).isFalse();
        assertThat(deleted.isDeleted()).isTrue();
    }
}
