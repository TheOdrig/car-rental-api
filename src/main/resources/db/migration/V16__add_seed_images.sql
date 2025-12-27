UPDATE gallery.car SET
    image_url = 'https://placehold.co/800x600/e2e8f0/475569?text=Volkswagen+Golf',
    thumbnail_url = 'https://placehold.co/400x300/e2e8f0/475569?text=VW+Golf'
WHERE license_plate = '34ABC123';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/1e3a5f/ffffff?text=BMW+320i',
    thumbnail_url = 'https://placehold.co/400x300/1e3a5f/ffffff?text=BMW+320i'
WHERE license_plate = '06DEF456';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/333333/ffffff?text=Audi+A4',
    thumbnail_url = 'https://placehold.co/400x300/333333/ffffff?text=Audi+A4'
WHERE license_plate = '35JKL789';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/0066cc/ffffff?text=Honda+Civic',
    thumbnail_url = 'https://placehold.co/400x300/0066cc/ffffff?text=Honda+Civic'
WHERE license_plate = '16XYZ321';

UPDATE gallery.car SET 
    image_url = 'https://placehold.co/800x600/cc0000/ffffff?text=Toyota+Corolla',
    thumbnail_url = 'https://placehold.co/400x300/cc0000/ffffff?text=Toyota+Corolla'
WHERE license_plate = '01MNO654';


INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34MRC001', 'WDD2050421A000006', 'Mercedes', 'C200', 2022,
    1850000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Sedan', 'Silver', 25000, 4, 5,
    TRUE, TRUE, 150, 45,
    'https://placehold.co/800x600/c0c0c0/333333?text=Mercedes+C200',
    'https://placehold.co/400x300/c0c0c0/333333?text=Mercedes+C200'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34TSL002', '5YJ3E1EA5KF000007', 'Tesla', 'Model 3', 2023,
    2250000.00, 'TRY', 0, 'AVAILABLE',
    'Electric', 'Automatic', 'Sedan', 'White', 12000, 4, 5,
    TRUE, TRUE, 320, 89,
    'https://placehold.co/800x600/f5f5f5/333333?text=Tesla+Model+3',
    'https://placehold.co/400x300/f5f5f5/333333?text=Tesla+Model+3'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06FRD003', '1FAHP3F2XCL000008', 'Ford', 'Focus', 2019,
    485000.00, 'TRY', 0, 'AVAILABLE',
    'Diesel', 'Manual', 'Hatchback', 'Blue', 78000, 5, 5,
    FALSE, TRUE, 85, 12,
    'https://placehold.co/800x600/1e40af/ffffff?text=Ford+Focus',
    'https://placehold.co/400x300/1e40af/ffffff?text=Ford+Focus'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35FIA004', 'ZFA35600000000009', 'Fiat', 'Egea', 2021,
    520000.00, 'TRY', 0, 'RENTED',
    'Diesel', 'Manual', 'Sedan', 'White', 42000, 4, 5,
    FALSE, FALSE, 95, 18,
    'https://placehold.co/800x600/dc2626/ffffff?text=Fiat+Egea',
    'https://placehold.co/400x300/dc2626/ffffff?text=Fiat+Egea'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16RNL005', 'VF1RFB00X5Y000010', 'Renault', 'Clio', 2020,
    395000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Hatchback', 'Orange', 55000, 5, 5,
    FALSE, TRUE, 72, 15,
    'https://placehold.co/800x600/ea580c/ffffff?text=Renault+Clio',
    'https://placehold.co/400x300/ea580c/ffffff?text=Renault+Clio'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34HYN006', 'KMHJ3814AKU000011', 'Hyundai', 'Tucson', 2022,
    1150000.00, 'TRY', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'SUV', 'Green', 35000, 5, 5,
    TRUE, TRUE, 180, 52,
    'https://placehold.co/800x600/166534/ffffff?text=Hyundai+Tucson',
    'https://placehold.co/400x300/166534/ffffff?text=Hyundai+Tucson'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06KIA007', 'KNAPH81ABC5000012', 'Kia', 'Sportage', 2021,
    985000.00, 'TRY', 0, 'RESERVED',
    'Diesel', 'Automatic', 'SUV', 'Gray', 48000, 5, 5,
    FALSE, FALSE, 125, 38,
    'https://placehold.co/800x600/6b7280/ffffff?text=Kia+Sportage',
    'https://placehold.co/400x300/6b7280/ffffff?text=Kia+Sportage'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35MZD008', 'JM1BN1L36E1000013', 'Mazda', '3', 2020,
    695000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Hatchback', 'Red', 52000, 5, 5,
    FALSE, TRUE, 88, 22,
    'https://placehold.co/800x600/b91c1c/ffffff?text=Mazda+3',
    'https://placehold.co/400x300/b91c1c/ffffff?text=Mazda+3'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34PGT009', 'VF3LBHZS6JS000014', 'Peugeot', '308', 2018,
    425000.00, 'TRY', 0, 'MAINTENANCE',
    'Diesel', 'Manual', 'Hatchback', 'Black', 98000, 5, 5,
    FALSE, FALSE, 45, 8,
    'https://placehold.co/800x600/1f2937/ffffff?text=Peugeot+308',
    'https://placehold.co/400x300/1f2937/ffffff?text=Peugeot+308'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06VLV010', 'YV1DZ8256C2000015', 'Volvo', 'XC60', 2022,
    2150000.00, 'TRY', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'SUV', 'Navy', 28000, 5, 5,
    TRUE, TRUE, 210, 65,
    'https://placehold.co/800x600/1e3a8a/ffffff?text=Volvo+XC60',
    'https://placehold.co/400x300/1e3a8a/ffffff?text=Volvo+XC60'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16SKD011', 'TMBEG41U0B2000016', 'Skoda', 'Octavia', 2021,
    785000.00, 'TRY', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Sedan', 'Brown', 38000, 4, 5,
    FALSE, TRUE, 98, 28,
    'https://placehold.co/800x600/78350f/ffffff?text=Skoda+Octavia',
    'https://placehold.co/400x300/78350f/ffffff?text=Skoda+Octavia'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34NSN012', 'SJNFAAJ11U2000017', 'Nissan', 'Qashqai', 2020,
    875000.00, 'TRY', 0, 'RENTED',
    'Diesel', 'Automatic', 'SUV', 'White', 55000, 5, 5,
    FALSE, FALSE, 135, 42,
    'https://placehold.co/800x600/e5e7eb/333333?text=Nissan+Qashqai',
    'https://placehold.co/400x300/e5e7eb/333333?text=Nissan+Qashqai'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35SEA013', 'VSSZZZ5FZJR000018', 'Seat', 'Leon', 2019,
    545000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Manual', 'Hatchback', 'Yellow', 62000, 5, 5,
    FALSE, TRUE, 65, 14,
    'https://placehold.co/800x600/eab308/333333?text=Seat+Leon',
    'https://placehold.co/400x300/eab308/333333?text=Seat+Leon'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06OPL014', 'W0L000000Y2000019', 'Opel', 'Astra', 2020,
    495000.00, 'TRY', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Hatchback', 'Purple', 48000, 5, 5,
    FALSE, TRUE, 78, 19,
    'https://placehold.co/800x600/7c3aed/ffffff?text=Opel+Astra',
    'https://placehold.co/400x300/7c3aed/ffffff?text=Opel+Astra'
) ON CONFLICT (license_plate) DO NOTHING;

INSERT INTO gallery.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    is_featured, is_test_drive_available, view_count, like_count,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16CTR015', 'VF7SXHMZ6GW000020', 'Citroen', 'C3', 2021,
    385000.00, 'TRY', 0, 'AVAILABLE',
    'Gasoline', 'Manual', 'Hatchback', 'Turquoise', 32000, 5, 5,
    FALSE, TRUE, 58, 11,
    'https://placehold.co/800x600/0d9488/ffffff?text=Citroen+C3',
    'https://placehold.co/400x300/0d9488/ffffff?text=Citroen+C3'
) ON CONFLICT (license_plate) DO NOTHING;
