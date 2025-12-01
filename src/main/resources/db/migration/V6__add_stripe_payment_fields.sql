ALTER TABLE gallery.payments
    ADD COLUMN IF NOT EXISTS stripe_session_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS stripe_payment_intent_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(255),
    ADD COLUMN IF NOT EXISTS refunded_amount DECIMAL(12,2),
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(500);

CREATE TABLE IF NOT EXISTS gallery.webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL,
    processed_at TIMESTAMP(6),
    error_message VARCHAR(1000),
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_webhook_event_id ON gallery.webhook_events(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_status ON gallery.webhook_events(status);
CREATE INDEX IF NOT EXISTS idx_webhook_created ON gallery.webhook_events(create_time);
