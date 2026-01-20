-- =============================================================================
-- V5: Damage Reports and Penalty Waivers
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Damage Reports Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.damage_reports (
    id BIGSERIAL PRIMARY KEY,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    rental_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,

    -- Car snapshot
    car_brand VARCHAR(50) NOT NULL,
    car_model VARCHAR(50) NOT NULL,
    car_license_plate VARCHAR(11) NOT NULL,

    -- Rental period
    rental_start_date DATE NOT NULL,
    rental_end_date DATE NOT NULL,
    
    -- Customer info
    customer_email VARCHAR(255) NOT NULL,
    customer_full_name VARCHAR(255) NOT NULL,
    customer_user_id BIGINT NOT NULL,

    -- Damage details
    description VARCHAR(1000) NOT NULL,
    damage_location VARCHAR(200),
    severity VARCHAR(20),
    category VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTED',

    -- Reporting
    reported_by BIGINT NOT NULL,
    reported_at TIMESTAMP(6) NOT NULL,

    -- Assessment
    assessed_by BIGINT,
    assessed_at TIMESTAMP(6),
    repair_cost_estimate DECIMAL(12, 2),
    customer_liability DECIMAL(12, 2),
    insurance_coverage BOOLEAN,
    insurance_deductible DECIMAL(12, 2),
    assessment_notes VARCHAR(1000),

    -- Payment
    payment_id BIGINT,
    payment_status VARCHAR(20),
    transaction_id VARCHAR(100),

    -- Dispute
    dispute_reason VARCHAR(500),
    dispute_comments VARCHAR(1000),
    disputed_by BIGINT,
    disputed_at TIMESTAMP(6),

    -- Resolution
    resolution_notes VARCHAR(1000),
    resolved_by BIGINT,
    resolved_at TIMESTAMP(6)
);

-- -----------------------------------------------------------------------------
-- Damage Photos Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.damage_photos (
    id BIGSERIAL PRIMARY KEY,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),

    damage_report_id BIGINT NOT NULL,

    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(100),

    uploaded_by BIGINT NOT NULL,
    uploaded_at TIMESTAMP(6) NOT NULL,
    display_order INTEGER,

    CONSTRAINT fk_damage_photo_report FOREIGN KEY (damage_report_id) REFERENCES gallery.damage_reports(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Penalty Waivers Table
-- -----------------------------------------------------------------------------
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

-- Indexes
CREATE INDEX IF NOT EXISTS idx_damage_reports_rental ON gallery.damage_reports(rental_id);
CREATE INDEX IF NOT EXISTS idx_damage_reports_car ON gallery.damage_reports(car_id);
CREATE INDEX IF NOT EXISTS idx_damage_reports_customer ON gallery.damage_reports(customer_user_id);
CREATE INDEX IF NOT EXISTS idx_damage_reports_status ON gallery.damage_reports(status);
CREATE INDEX IF NOT EXISTS idx_damage_reports_reported_at ON gallery.damage_reports(reported_at);
CREATE INDEX IF NOT EXISTS idx_damage_photos_report ON gallery.damage_photos(damage_report_id);
CREATE INDEX IF NOT EXISTS idx_penalty_waiver_rental ON gallery.penalty_waivers(rental_id);
CREATE INDEX IF NOT EXISTS idx_penalty_waiver_admin ON gallery.penalty_waivers(admin_id);
