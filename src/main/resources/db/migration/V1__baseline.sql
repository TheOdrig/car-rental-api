CREATE SCHEMA IF NOT EXISTS car_rental;

CREATE TABLE car_rental.car (
    id BIGSERIAL PRIMARY KEY,
    license_plate VARCHAR(11) NOT NULL,
    vin_number VARCHAR(17),
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    production_year INTEGER NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency_type VARCHAR(10) NOT NULL,
    damage_price NUMERIC(12,2) DEFAULT 0,
    car_status_type VARCHAR(15) NOT NULL,
    engine_type VARCHAR(20),
    engine_displacement NUMERIC(4,2),
    fuel_type VARCHAR(20),
    transmission_type VARCHAR(20),
    body_type VARCHAR(20),
    color VARCHAR(30),
    kilometer BIGINT,
    doors INTEGER,
    seats INTEGER,
    registration_date DATE,
    last_service_date DATE,
    next_service_date DATE,
    insurance_expiry_date DATE,
    inspection_expiry_date DATE,
    notes VARCHAR(1000),
    image_url VARCHAR(500),
    thumbnail_url VARCHAR(500),
    is_featured BOOLEAN NOT NULL DEFAULT FALSE,
    is_test_drive_available BOOLEAN NOT NULL DEFAULT TRUE,
    rating NUMERIC(2,1),
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    CONSTRAINT uk_car_license_plate UNIQUE (license_plate),
    CONSTRAINT uk_car_vin UNIQUE (vin_number)
);

CREATE INDEX idx_car_brand ON car_rental.car (brand);
CREATE INDEX idx_car_status ON car_rental.car (car_status_type);
CREATE INDEX idx_car_price ON car_rental.car (price);
CREATE INDEX index_car_year ON car_rental.car (production_year);
CREATE INDEX index_car_create_time ON car_rental.car (create_time);
CREATE INDEX idx_car_transmission_type ON car_rental.car (transmission_type);
CREATE INDEX idx_car_body_type ON car_rental.car (body_type);
CREATE INDEX idx_car_fuel_type ON car_rental.car (fuel_type);
CREATE INDEX idx_car_seats ON car_rental.car (seats);

CREATE TABLE car_rental.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    auth_provider VARCHAR(20) DEFAULT 'LOCAL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    banned_at TIMESTAMP,
    banned_by BIGINT,
    ban_reason VARCHAR(500),
    unbanned_at TIMESTAMP,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE INDEX idx_users_enabled ON car_rental.users (enabled);

CREATE TABLE car_rental.user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES car_rental.users (id) ON DELETE CASCADE
);

CREATE TABLE car_rental.linked_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_linked_account_user FOREIGN KEY (user_id) REFERENCES car_rental.users (id) ON DELETE CASCADE,
    CONSTRAINT uq_linked_account_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX idx_linked_accounts_user_id ON car_rental.linked_accounts (user_id);
CREATE INDEX idx_linked_accounts_provider_email ON car_rental.linked_accounts (provider, email);

CREATE TABLE car_rental.password_reset_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_password_reset_tokens_token UNIQUE (token)
);

CREATE INDEX idx_password_reset_tokens_email ON car_rental.password_reset_tokens (email);
CREATE INDEX idx_password_reset_tokens_expiry ON car_rental.password_reset_tokens (expiry_date);

CREATE TABLE car_rental.admin_notes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    admin_username VARCHAR(50) NOT NULL,
    text VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_admin_note_user FOREIGN KEY (user_id) REFERENCES car_rental.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_admin_note_admin FOREIGN KEY (admin_id) REFERENCES car_rental.users (id)
);

CREATE INDEX idx_admin_notes_user ON car_rental.admin_notes (user_id);
CREATE INDEX idx_admin_notes_admin ON car_rental.admin_notes (admin_id);
CREATE INDEX idx_admin_notes_created ON car_rental.admin_notes (created_at);

CREATE TABLE car_rental.rentals (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    car_brand VARCHAR(50) NOT NULL,
    car_model VARCHAR(50) NOT NULL,
    car_license_plate VARCHAR(11) NOT NULL,
    car_thumbnail_url VARCHAR(500),
    user_email VARCHAR(255) NOT NULL,
    user_full_name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    days INTEGER NOT NULL,
    currency VARCHAR(10) NOT NULL,
    daily_price NUMERIC(12,2) NOT NULL,
    total_price NUMERIC(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pickup_notes TEXT,
    return_notes TEXT,
    approval_notes TEXT,
    cancellation_reason TEXT,
    pickup_reminder_sent BOOLEAN DEFAULT FALSE,
    return_reminder_sent BOOLEAN DEFAULT FALSE,
    late_return_status VARCHAR(20),
    late_detected_at TIMESTAMP(6),
    actual_return_time TIMESTAMP(6),
    late_hours INTEGER,
    penalty_amount NUMERIC(12,2),
    penalty_paid BOOLEAN DEFAULT FALSE,
    has_damage_reports BOOLEAN NOT NULL DEFAULT FALSE,
    damage_reports_count INTEGER NOT NULL DEFAULT 0,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_rental_user FOREIGN KEY (user_id) REFERENCES car_rental.users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_rental_car FOREIGN KEY (car_id) REFERENCES car_rental.car (id) ON DELETE RESTRICT,
    CONSTRAINT chk_rental_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_rental_days CHECK (days > 0),
    CONSTRAINT chk_rental_prices CHECK (daily_price > 0 AND total_price > 0),
    CONSTRAINT chk_penalty_amount CHECK (penalty_amount IS NULL OR penalty_amount >= 0),
    CONSTRAINT chk_late_hours CHECK (late_hours IS NULL OR late_hours >= 0)
);

CREATE INDEX idx_rentals_car ON car_rental.rentals (car_id);
CREATE INDEX idx_rentals_user ON car_rental.rentals (user_id);
CREATE INDEX idx_rentals_status ON car_rental.rentals (status);
CREATE INDEX idx_rentals_dates ON car_rental.rentals (start_date, end_date);
CREATE INDEX idx_rentals_car_license_plate ON car_rental.rentals (car_license_plate);
CREATE INDEX idx_rentals_user_email ON car_rental.rentals (user_email);
CREATE INDEX idx_rentals_late_status ON car_rental.rentals (late_return_status);
CREATE INDEX idx_rentals_has_damage ON car_rental.rentals (has_damage_reports) WHERE has_damage_reports = TRUE;

CREATE TABLE car_rental.payments (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(50),
    transaction_id VARCHAR(255),
    gateway_response TEXT,
    stripe_session_id VARCHAR(255),
    stripe_payment_intent_id VARCHAR(255),
    idempotency_key VARCHAR(255),
    refunded_amount NUMERIC(12,2),
    failure_reason VARCHAR(500),
    user_email VARCHAR(255),
    car_license_plate VARCHAR(20),
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_payment_rental FOREIGN KEY (rental_id) REFERENCES car_rental.rentals (id) ON DELETE CASCADE,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payments_rental ON car_rental.payments (rental_id);
CREATE INDEX idx_payments_status ON car_rental.payments (status);
CREATE INDEX idx_payments_stripe_session ON car_rental.payments (stripe_session_id);
CREATE INDEX idx_payments_stripe_intent ON car_rental.payments (stripe_payment_intent_id);

CREATE TABLE car_rental.webhook_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(255) NOT NULL,
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
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_webhook_event_id UNIQUE (event_id)
);

CREATE INDEX idx_webhook_status ON car_rental.webhook_events (status);
CREATE INDEX idx_webhook_created ON car_rental.webhook_events (create_time);

CREATE TABLE car_rental.damage_reports (
    id BIGSERIAL PRIMARY KEY,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    rental_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    car_brand VARCHAR(50) NOT NULL,
    car_model VARCHAR(50) NOT NULL,
    car_license_plate VARCHAR(11) NOT NULL,
    rental_start_date DATE NOT NULL,
    rental_end_date DATE NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    customer_full_name VARCHAR(255) NOT NULL,
    customer_user_id BIGINT NOT NULL,
    description VARCHAR(1000) NOT NULL,
    damage_location VARCHAR(200),
    severity VARCHAR(20),
    category VARCHAR(30),
    status VARCHAR(20) NOT NULL DEFAULT 'REPORTED',
    reported_by BIGINT NOT NULL,
    reported_at TIMESTAMP(6) NOT NULL,
    assessed_by BIGINT,
    assessed_at TIMESTAMP(6),
    repair_cost_estimate NUMERIC(12,2),
    customer_liability NUMERIC(12,2),
    insurance_coverage BOOLEAN,
    insurance_deductible NUMERIC(12,2),
    assessment_notes VARCHAR(1000),
    payment_id BIGINT,
    payment_status VARCHAR(20),
    transaction_id VARCHAR(100),
    dispute_reason VARCHAR(500),
    dispute_comments VARCHAR(1000),
    disputed_by BIGINT,
    disputed_at TIMESTAMP(6),
    resolution_notes VARCHAR(1000),
    resolved_by BIGINT,
    resolved_at TIMESTAMP(6)
);

CREATE INDEX idx_damage_reports_rental ON car_rental.damage_reports (rental_id);
CREATE INDEX idx_damage_reports_car ON car_rental.damage_reports (car_id);
CREATE INDEX idx_damage_reports_customer ON car_rental.damage_reports (customer_user_id);
CREATE INDEX idx_damage_reports_status ON car_rental.damage_reports (status);
CREATE INDEX idx_damage_reports_reported_at ON car_rental.damage_reports (reported_at);

CREATE TABLE car_rental.damage_photos (
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
    CONSTRAINT fk_damage_photo_report FOREIGN KEY (damage_report_id) REFERENCES car_rental.damage_reports (id) ON DELETE CASCADE
);

CREATE INDEX idx_damage_photos_report ON car_rental.damage_photos (damage_report_id);

CREATE TABLE car_rental.penalty_waivers (
    id BIGSERIAL PRIMARY KEY,
    rental_id BIGINT NOT NULL,
    original_penalty NUMERIC(12,2) NOT NULL,
    waived_amount NUMERIC(12,2) NOT NULL,
    remaining_penalty NUMERIC(12,2) NOT NULL,
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
    CONSTRAINT fk_penalty_waiver_rental FOREIGN KEY (rental_id) REFERENCES car_rental.rentals (id) ON DELETE CASCADE,
    CONSTRAINT chk_waiver_amounts CHECK (
        original_penalty >= 0
        AND waived_amount >= 0
        AND remaining_penalty >= 0
        AND waived_amount <= original_penalty
        AND remaining_penalty = original_penalty - waived_amount
    )
);

CREATE INDEX idx_penalty_waiver_rental ON car_rental.penalty_waivers (rental_id);
CREATE INDEX idx_penalty_waiver_admin ON car_rental.penalty_waivers (admin_id);

CREATE TABLE car_rental.dashboard_alerts (
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

CREATE INDEX idx_alert_type ON car_rental.dashboard_alerts (type);
CREATE INDEX idx_alert_severity ON car_rental.dashboard_alerts (severity);
CREATE INDEX idx_alert_acknowledged ON car_rental.dashboard_alerts (acknowledged);
CREATE INDEX idx_alert_created ON car_rental.dashboard_alerts (create_time);
CREATE INDEX idx_alert_active ON car_rental.dashboard_alerts (acknowledged, severity) WHERE acknowledged = FALSE;
