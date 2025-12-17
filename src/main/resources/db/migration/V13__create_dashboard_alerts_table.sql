CREATE TABLE dashboard_alerts (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    action_url VARCHAR(500),
    reference_id BIGINT,
    acknowledged BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_at TIMESTAMP,
    acknowledged_by VARCHAR(100),
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100)
);

CREATE INDEX idx_alert_type ON dashboard_alerts(type);
CREATE INDEX idx_alert_severity ON dashboard_alerts(severity);
CREATE INDEX idx_alert_acknowledged ON dashboard_alerts(acknowledged);
CREATE INDEX idx_alert_created ON dashboard_alerts(create_time);

CREATE INDEX idx_alert_active ON dashboard_alerts(acknowledged, severity) WHERE acknowledged = FALSE;

COMMENT ON TABLE dashboard_alerts IS 'System-generated alerts for admin dashboard';
COMMENT ON COLUMN dashboard_alerts.type IS 'Alert category: LATE_RETURN, FAILED_PAYMENT, LOW_AVAILABILITY, UNRESOLVED_DISPUTE, MAINTENANCE_REQUIRED';
COMMENT ON COLUMN dashboard_alerts.severity IS 'Priority level: CRITICAL(1), HIGH(2), WARNING(3), MEDIUM(4), LOW(5)';
COMMENT ON COLUMN dashboard_alerts.reference_id IS 'Related entity ID (rental, payment, etc.)';

