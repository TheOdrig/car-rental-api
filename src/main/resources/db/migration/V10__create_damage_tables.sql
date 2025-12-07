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

    description VARCHAR(1000) NOT NULL,
    damage_location VARCHAR(200),
    severity VARCHAR(20),
    category VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTED',

    reported_by BIGINT NOT NULL,
    reported_at TIMESTAMP(6) NOT NULL,

    assessed_by BIGINT,
    assessed_at TIMESTAMP(6),
    repair_cost_estimate DECIMAL(12, 2),
    customer_liability DECIMAL(12, 2),
    insurance_coverage BOOLEAN,
    insurance_deductible DECIMAL(12, 2),
    assessment_notes VARCHAR(1000),

    payment_id BIGINT,
    payment_status VARCHAR(20),

    dispute_reason VARCHAR(500),
    dispute_comments VARCHAR(1000),
    disputed_by BIGINT,
    disputed_at TIMESTAMP(6),

    resolution_notes VARCHAR(1000),
    resolved_by BIGINT,
    resolved_at TIMESTAMP(6),

    CONSTRAINT fk_damage_report_rental FOREIGN KEY (rental_id) REFERENCES gallery.rentals(id) ON DELETE RESTRICT,
    CONSTRAINT fk_damage_report_car FOREIGN KEY (car_id) REFERENCES gallery.car(id) ON DELETE RESTRICT
);

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

CREATE INDEX IF NOT EXISTS idx_damage_reports_rental ON gallery.damage_reports(rental_id);
CREATE INDEX IF NOT EXISTS idx_damage_reports_car ON gallery.damage_reports(car_id);
CREATE INDEX IF NOT EXISTS idx_damage_reports_status ON gallery.damage_reports(status);
CREATE INDEX IF NOT EXISTS idx_damage_reports_reported_at ON gallery.damage_reports(reported_at);

CREATE INDEX IF NOT EXISTS idx_damage_photos_report ON gallery.damage_photos(damage_report_id);
