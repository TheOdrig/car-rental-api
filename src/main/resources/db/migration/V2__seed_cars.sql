INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count
) VALUES
(
    NOW(), NOW(), FALSE, 0,
    '34ABC123', 'WVWZZZ1JZXW000001', 'Volkswagen', 'Golf', 2018,
    345000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Manuel', 'Hatchback', 'White', 85000, 5, 5,
    TRUE, TRUE, 0, 0
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count
) VALUES
(
    NOW(), NOW(), FALSE, 0,
    '06DEF456', 'WBA3A51000F000002', 'BMW', '320i', 2020,
    1250000.00, 'TRY', 0, 'RESERVED',
    'Diesel', 'Automatic', 'Sedan', 'Black', 45000, 4, 5,
    TRUE, TRUE, 0, 0
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count
) VALUES
(
    NOW(), NOW(), FALSE, 0,
    '35JKL789', 'WAUZZZ8K9AA000003', 'Audi', 'A4', 2017,
    950000.00, 'TRY', 0, 'SOLD',
    'Diesel', 'Automatic', 'Sedan', 'Gray', 120000, 4, 5,
    FALSE, FALSE, 0, 0
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count
) VALUES
(
    NOW(), NOW(), FALSE, 0,
    '16XYZ321', '1HGBH41JXMN109186', 'Honda', 'Civic', 2019,
    625000.00, 'TRY', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'Sedan', 'Blue', 62000, 4, 5,
    TRUE, TRUE, 0, 0
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count
) VALUES
(
    NOW(), NOW(), FALSE, 0,
    '01MNO654', 'JTDKBRFU9H3511234', 'Toyota', 'Corolla', 2021,
    785000.00, 'TRY', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'Sedan', 'Red', 28000, 4, 5,
    TRUE, TRUE, 0, 0
) ON CONFLICT (license_plate) DO NOTHING;

