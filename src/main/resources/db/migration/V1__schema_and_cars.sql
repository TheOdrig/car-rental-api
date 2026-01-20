-- =============================================================================
-- V1: Schema, Car Table, and Indexes
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS gallery;

-- -----------------------------------------------------------------------------
-- Car Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.car (
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
    updated_by VARCHAR(100)
);

-- Unique Constraints
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_car_license_plate') THEN
        ALTER TABLE gallery.car ADD CONSTRAINT uk_car_license_plate UNIQUE (license_plate);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_car_vin') THEN
        ALTER TABLE gallery.car ADD CONSTRAINT uk_car_vin UNIQUE (vin_number);
    END IF;
END $$;

-- Indexes
CREATE INDEX IF NOT EXISTS idx_car_brand ON gallery.car(brand);
CREATE INDEX IF NOT EXISTS idx_car_status ON gallery.car(car_status_type);
CREATE INDEX IF NOT EXISTS idx_car_price ON gallery.car(price);
CREATE INDEX IF NOT EXISTS index_car_year ON gallery.car(production_year);
CREATE INDEX IF NOT EXISTS index_car_create_time ON gallery.car(create_time);
CREATE INDEX IF NOT EXISTS idx_car_transmission_type ON gallery.car(transmission_type);
CREATE INDEX IF NOT EXISTS idx_car_body_type ON gallery.car(body_type);
CREATE INDEX IF NOT EXISTS idx_car_fuel_type ON gallery.car(fuel_type);
CREATE INDEX IF NOT EXISTS idx_car_seats ON gallery.car(seats);
