package com.akif.model;

import com.akif.enums.WebhookEventStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events",
        indexes = {
                @Index(name = "idx_webhook_event_id", columnList = "event_id", unique = true),
                @Index(name = "idx_webhook_status", columnList = "status"),
                @Index(name = "idx_webhook_created", columnList = "create_time")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class WebhookEvent extends BaseEntity {

    @Column(name = "event_id", unique = true, nullable = false, length = 255)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private WebhookEventStatus status = WebhookEventStatus.RECEIVED;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Override
    public String toString() {
        return "WebhookEvent{" +
                "id=" + getId() +
                ", eventId='" + eventId + '\'' +
                ", eventType='" + eventType + '\'' +
                ", status=" + status +
                ", processedAt=" + processedAt +
                '}';
    }
}
