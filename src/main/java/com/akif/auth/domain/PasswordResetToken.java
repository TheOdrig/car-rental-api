package com.akif.auth.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_password_reset_tokens_token", columnList = "token"),
        @Index(name = "idx_password_reset_tokens_email", columnList = "email"),
        @Index(name = "idx_password_reset_tokens_expiry", columnList = "expiry_date")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "used", nullable = false)
    @Builder.Default
    private Boolean used = false;

    @Column(name = "created_at", nullable = false)
    @CreatedDate
    private Instant createdAt;

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiryDate);
    }

    public boolean isValidAt(Instant now) {
        return !used && !isExpiredAt(now);
    }

    public void markAsUsed() {
        this.used = true;
    }
}
