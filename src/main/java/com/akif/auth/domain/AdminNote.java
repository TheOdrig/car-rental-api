package com.akif.auth.domain;

import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notes", indexes = {
        @Index(name = "idx_admin_notes_user", columnList = "user_id"),
        @Index(name = "idx_admin_notes_admin", columnList = "admin_id"),
        @Index(name = "idx_admin_notes_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AdminNote extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername;

    @Column(name = "text", nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
