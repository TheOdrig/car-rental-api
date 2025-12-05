CREATE TABLE IF NOT EXISTS gallery.penalty_waivers (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL,
    original_penalty DECIMAL(12,2) NOT NULL,
    waived_amount DECIMAL(12,2) NOT NULL,
    remaining_penalty DECIMAL(12,2) NOT NULL,
    reason TEXT NOT NULL,
    admin_id BIGINT NOT NULL,
    waived_at TIMESTAMP(6) NOT NULL,
    refund_initiated BOOLEAN,
    refund_transaction_id VARCHAR(255),
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_penalty_waiver_rental FOREIGN KEY (rental_id) REFERENCES gallery.rentals(id) ON DELETE CASCADE,
    CONSTRAINT chk_waiver_amounts CHECK (
        original_penalty >= 0 AND 
        waived_amount >= 0 AND 
        remaining_penalty >= 0 AND
        waived_amount <= original_penalty AND
        remaining_penalty = original_penalty - waived_amount
    )
);

CREATE INDEX IF NOT EXISTS idx_penalty_waiver_rental ON gallery.penalty_waivers(rental_id);
CREATE INDEX IF NOT EXISTS idx_penalty_waiver_admin ON gallery.penalty_waivers(admin_id);
