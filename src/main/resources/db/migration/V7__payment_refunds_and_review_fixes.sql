ALTER TABLE car_rental.payments
    ADD COLUMN last_synced_at TIMESTAMP(6);

CREATE TABLE car_rental.payment_refunds (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL,
    operation_key VARCHAR(100) NOT NULL,
    stripe_refund_id VARCHAR(255),
    amount NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payment_refund_payment FOREIGN KEY (payment_id) REFERENCES car_rental.payments (id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_refund_amount CHECK (amount > 0),
    CONSTRAINT chk_payment_refund_status CHECK (status IN ('SUCCEEDED', 'FAILED'))
);

CREATE INDEX idx_payment_refunds_payment ON car_rental.payment_refunds (payment_id);

CREATE UNIQUE INDEX uk_payment_refunds_stripe_refund
    ON car_rental.payment_refunds (stripe_refund_id)
    WHERE stripe_refund_id IS NOT NULL;

CREATE UNIQUE INDEX uk_payment_refunds_active_operation
    ON car_rental.payment_refunds (payment_id, operation_key)
    WHERE status = 'SUCCEEDED';

ALTER TABLE car_rental.damage_reports
    ADD COLUMN currency VARCHAR(10);

UPDATE car_rental.damage_reports d
SET currency = r.currency
FROM car_rental.rentals r
WHERE r.id = d.rental_id;
