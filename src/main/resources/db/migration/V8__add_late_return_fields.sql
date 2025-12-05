ALTER TABLE gallery.rentals
    ADD COLUMN late_return_status VARCHAR(20),
    ADD COLUMN late_detected_at TIMESTAMP(6),
    ADD COLUMN actual_return_time TIMESTAMP(6),
    ADD COLUMN late_hours INTEGER,
    ADD COLUMN penalty_amount DECIMAL(12,2),
    ADD COLUMN penalty_paid BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_rentals_late_status ON gallery.rentals(late_return_status);

ALTER TABLE gallery.rentals
    ADD CONSTRAINT chk_penalty_amount CHECK (penalty_amount IS NULL OR penalty_amount >= 0);

ALTER TABLE gallery.rentals
    ADD CONSTRAINT chk_late_hours CHECK (late_hours IS NULL OR late_hours >= 0);
