CREATE SCHEMA IF NOT EXISTS gallery;

CREATE TABLE gallery.car (

    id BIGSERIAL PRIMARY KEY,

    license_plate VARCHAR(11) NOT NULL,
    vin_number VARCHAR(17),
    brand VARCHAR(50) NOT NULL,
    model VARCHAR(50) NOT NULL,
    production_year INTEGER NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    currency_type VARCHAR(10) NOT NULL,
    damage_price NUMERIC(12,2) DEFAULT 0,
    car_status_type VARCHAR(10) NOT NULL,

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

ALTER TABLE gallery.car ADD CONSTRAINT uk_car_license_plate UNIQUE (license_plate);
ALTER TABLE gallery.car ADD CONSTRAINT uk_car_vin UNIQUE (vin_number);

CREATE INDEX idx_car_brand ON gallery.car(brand);
CREATE INDEX idx_car_status ON gallery.car(car_status_type);
CREATE INDEX idx_car_price ON gallery.car(price);
CREATE INDEX index_car_year ON gallery.car(production_year);
CREATE INDEX index_car_create_time ON gallery.car(create_time);

