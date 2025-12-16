package com.akif.dashboard.domain.model;

import com.akif.dashboard.domain.enums.AlertSeverity;
import com.akif.dashboard.domain.enums.AlertType;
import com.akif.shared.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_alerts",
    indexes = {
        @Index(name = "idx_alert_type", columnList = "type"),
        @Index(name = "idx_alert_severity", columnList = "severity"),
        @Index(name = "idx_alert_acknowledged", columnList = "acknowledged"),
        @Index(name = "idx_alert_created", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Alert extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AlertSeverity severity;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(name = "acknowledged", nullable = false)
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void acknowledge(String adminUsername) {
        this.acknowledged = true;
        this.acknowledgedAt = LocalDateTime.now();
        this.acknowledgedBy = adminUsername;
    }

    public boolean isAcknowledged() {
        return Boolean.TRUE.equals(this.acknowledged);
    }
}
