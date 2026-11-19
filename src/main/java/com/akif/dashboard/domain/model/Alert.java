package com.akif.dashboard.domain.model;

import com.akif.dashboard.api.AlertSeverity;
import com.akif.dashboard.api.AlertType;
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
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "dashboard_alerts",
    indexes = {
        @Index(name = "idx_alert_type", columnList = "type"),
        @Index(name = "idx_alert_created", columnList = "create_time")
    })
@Getter
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
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by", length = 100)
    private String acknowledgedBy;

    public void acknowledge(String adminUsername, Instant at) {
        this.acknowledged = true;
        this.acknowledgedAt = at;
        this.acknowledgedBy = adminUsername;
    }

    public boolean isAcknowledged() {
        return Boolean.TRUE.equals(this.acknowledged);
    }
}

