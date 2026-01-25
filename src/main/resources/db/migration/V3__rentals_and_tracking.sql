-- =============================================================================
-- V3: Rentals Table with All Tracking Fields
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Rentals Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.rentals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,

    -- Car snapshot
    car_brand VARCHAR(50) NOT NULL,
    car_model VARCHAR(50) NOT NULL,
    car_license_plate VARCHAR(11) NOT NULL,
    car_thumbnail_url VARCHAR(500),

    -- User snapshot
    user_email VARCHAR(255) NOT NULL,
    user_full_name VARCHAR(255) NOT NULL,

    -- Rental details
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL,
    daily_price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pickup_notes TEXT,
    return_notes TEXT,
    approval_notes TEXT,
    cancellation_reason TEXT,

    -- Reminder tracking
    pickup_reminder_sent BOOLEAN DEFAULT FALSE,
    return_reminder_sent BOOLEAN DEFAULT FALSE,

    -- Late return tracking
    late_return_status VARCHAR(20),
    late_detected_at TIMESTAMP(6),
    actual_return_time TIMESTAMP(6),
    late_hours INTEGER,
    penalty_amount DECIMAL(12,2),
    penalty_paid BOOLEAN DEFAULT FALSE,

    -- Damage tracking
    has_damage_reports BOOLEAN NOT NULL DEFAULT FALSE,
    damage_reports_count INTEGER NOT NULL DEFAULT 0,

    -- Audit fields
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT fk_rental_user FOREIGN KEY (user_id) REFERENCES gallery.users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_car FOREIGN KEY (car_id) REFERENCES gallery.car(id) ON DELETE RESTRICT,
    CONSTRAINT chk_rental_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_rental_days CHECK (days > 0),
    CONSTRAINT chk_rental_prices CHECK (daily_price > 0 AND total_price > 0),
    CONSTRAINT chk_penalty_amount CHECK (penalty_amount IS NULL OR penalty_amount >= 0),
    CONSTRAINT chk_late_hours CHECK (late_hours IS NULL OR late_hours >= 0)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_rentals_car ON gallery.rentals(car_id);
CREATE INDEX IF NOT EXISTS idx_rentals_user ON gallery.rentals(user_id);
CREATE INDEX IF NOT EXISTS idx_rentals_status ON gallery.rentals(status);
CREATE INDEX IF NOT EXISTS idx_rentals_dates ON gallery.rentals(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_rentals_car_license_plate ON gallery.rentals(car_license_plate);
CREATE INDEX IF NOT EXISTS idx_rentals_user_email ON gallery.rentals(user_email);
CREATE INDEX IF NOT EXISTS idx_rentals_late_status ON gallery.rentals(late_return_status);
CREATE INDEX IF NOT EXISTS idx_rentals_has_damage ON gallery.rentals(has_damage_reports) WHERE has_damage_reports = TRUE;
