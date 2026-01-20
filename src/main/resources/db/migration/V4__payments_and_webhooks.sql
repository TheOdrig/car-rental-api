-- =============================================================================
-- V4: Payments Table with Stripe Integration
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Payments Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.payments (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255),
    gateway_response TEXT,
    
    -- Stripe fields
    stripe_session_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),
    idempotency_key VARCHAR(255),
    refunded_amount DECIMAL(12,2),
    failure_reason VARCHAR(500),
    
    -- Additional tracking
    user_email VARCHAR(255),
    car_license_plate VARCHAR(20),
    
    -- Audit fields
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Constraints
    CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES gallery.rentals(id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

-- -----------------------------------------------------------------------------
-- Webhook Events Table (Stripe)
-- -----------------------------------------------------------------------------
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

-- Indexes
CREATE INDEX IF NOT EXISTS idx_payments_rental ON gallery.payments(rental_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON gallery.payments(status);
CREATE INDEX IF NOT EXISTS idx_webhook_event_id ON gallery.webhook_events(event_id);
CREATE INDEX IF NOT EXISTS idx_webhook_status ON gallery.webhook_events(status);
CREATE INDEX IF NOT EXISTS idx_webhook_created ON gallery.webhook_events(create_time);
