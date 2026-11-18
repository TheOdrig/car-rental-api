INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34ABC123', 'WVWZZZ1JZXW000001', 'Volkswagen', 'Golf', 2018,
    35.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Manual', 'Hatchback', 'White', 85000, 5, 5,
    'https://images.unsplash.com/photo-1718629879998-ee8cfc09df39?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1718629879998-ee8cfc09df39?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16RNL005', 'VF1RFB00X5Y000010', 'Renault', 'Clio', 2020,
    40.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Hatchback', 'Orange', 55000, 5, 5,
    'https://images.unsplash.com/photo-1594502225401-a9eab8b405dd?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1594502225401-a9eab8b405dd?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16CTR015', 'VF7SXHMZ6GW000020', 'Citroen', 'C3', 2021,
    38.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Manual', 'Hatchback', 'Turquoise', 32000, 5, 5,
    'https://images.unsplash.com/photo-1609030429269-ca5b7a409310?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1609030429269-ca5b7a409310?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16XYZ321', '1HGBH41JXMN109186', 'Honda', 'Civic', 2019,
    45.00, 'USD', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'Sedan', 'Blue', 62000, 4, 5,
    'https://images.unsplash.com/photo-1605515321331-46a63d31758c?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1605515321331-46a63d31758c?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06FRD003', '1FAHP3F2XCL000008', 'Ford', 'Focus', 2019,
    48.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Manual', 'Hatchback', 'Blue', 78000, 5, 5,
    'https://images.unsplash.com/photo-1708849894321-2c9bc515df0e?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1708849894321-2c9bc515df0e?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35FIA004', 'ZFA35600000000009', 'Fiat', 'Egea', 2021,
    42.00, 'USD', 0, 'RENTED',
    'Diesel', 'Manual', 'Sedan', 'White', 42000, 4, 5,
    'https://www.log.com.tr/wp-content/uploads/2023/01/2025-fiat-egea-tipo-suv-benzeri-bir-formda-olacak-copy-1000x562.jpg',
    'https://www.log.com.tr/wp-content/uploads/2023/01/2025-fiat-egea-tipo-suv-benzeri-bir-formda-olacak-copy-1000x562.jpg'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35MZD008', 'JM1BN1L36E1000013', 'Mazda', '3', 2020,
    55.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Hatchback', 'Red', 52000, 5, 5,
    'https://images.unsplash.com/photo-1643142311721-c36cd233fb95?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1643142311721-c36cd233fb95?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34PGT009', 'VF3LBHZS6JS000014', 'Peugeot', '308', 2018,
    40.00, 'USD', 0, 'MAINTENANCE',
    'Diesel', 'Manual', 'Hatchback', 'Black', 98000, 5, 5,
    'https://images.unsplash.com/photo-1757695526350-1a2db6ff8e02?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1757695526350-1a2db6ff8e02?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35SEA013', 'VSSZZZ5FZJR000018', 'Seat', 'Leon', 2019,
    45.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Manual', 'Hatchback', 'Yellow', 62000, 5, 5,
    'https://commons.wikimedia.org/wiki/Special:FilePath/SEAT_Leon_Cupra_002.jpg?width=800',
    'https://commons.wikimedia.org/wiki/Special:FilePath/SEAT_Leon_Cupra_002.jpg?width=800'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06OPL014', 'W0L000000Y2000019', 'Opel', 'Astra', 2020,
    42.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Hatchback', 'Gray', 48000, 5, 5,
    'https://images.unsplash.com/photo-1582639510494-c80b5de9f148?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1582639510494-c80b5de9f148?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '01MNO654', 'JTDKBRFU9H3511234', 'Toyota', 'Corolla', 2021,
    65.00, 'USD', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'Sedan', 'Red', 28000, 4, 5,
    'https://www.thedrive.com/wp-content/uploads/2023/05/02/corollahatch-1-scaled.jpg?w=1200',
    'https://www.thedrive.com/wp-content/uploads/2023/05/02/corollahatch-1-scaled.jpg?w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '16SKD011', 'TMBEG41U0B2000016', 'Skoda', 'Octavia', 2021,
    60.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Sedan', 'Brown', 38000, 4, 5,
    'https://images.unsplash.com/photo-1673822317394-6fd502a324e6?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1673822317394-6fd502a324e6?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34HYN006', 'KMHJ3814AKU000011', 'Hyundai', 'Tucson', 2022,
    85.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'SUV', 'Green', 35000, 5, 5,
    'https://images.unsplash.com/photo-1705624843697-4461f9dce482?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1705624843697-4461f9dce482?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06KIA007', 'KNAPH81ABC5000012', 'Kia', 'Sportage', 2021,
    80.00, 'USD', 0, 'RESERVED',
    'Diesel', 'Automatic', 'SUV', 'Gray', 48000, 5, 5,
    'https://images.unsplash.com/photo-1688893287585-b1bc00e608fc?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1688893287585-b1bc00e608fc?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34NSN012', 'SJNFAAJ11U2000017', 'Nissan', 'Qashqai', 2020,
    75.00, 'USD', 0, 'RENTED',
    'Diesel', 'Automatic', 'SUV', 'White', 55000, 5, 5,
    'https://images.unsplash.com/photo-1538940714252-fc70779afa46?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1538940714252-fc70779afa46?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06DEF456', 'WBA3A51000F000002', 'BMW', '320i', 2020,
    95.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Sedan', 'Black', 45000, 4, 5,
    'https://images.unsplash.com/photo-1639056067266-43a821cf0a1f?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1639056067266-43a821cf0a1f?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '35JKL789', 'WAUZZZ8K9AA000003', 'Audi', 'A4', 2017,
    90.00, 'USD', 0, 'AVAILABLE',
    'Diesel', 'Automatic', 'Sedan', 'Gray', 120000, 4, 5,
    'https://images.unsplash.com/photo-1710011115921-7e67a8e4b483?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1710011115921-7e67a8e4b483?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34MRC001', 'WDD2050421A000006', 'Mercedes', 'C200', 2022,
    120.00, 'USD', 0, 'AVAILABLE',
    'Gasoline', 'Automatic', 'Sedan', 'Silver', 25000, 4, 5,
    'https://www.sixt.com.tr/storage/cache/a4831ea1285880c2536797abe33a35a517ddeacf.webp',
    'https://www.sixt.com.tr/storage/cache/a4831ea1285880c2536797abe33a35a517ddeacf.webp'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '06VLV010', 'YV1DZ8256C2000015', 'Volvo', 'XC60', 2022,
    145.00, 'USD', 0, 'AVAILABLE',
    'Hybrid', 'Automatic', 'SUV', 'Navy', 28000, 5, 5,
    'https://images.unsplash.com/photo-1629897046038-371765238f26?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1629897046038-371765238f26?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;

INSERT INTO car_rental.car (
    create_time, update_time, is_deleted, version,
    license_plate, vin_number, brand, model, production_year,
    price, currency_type, damage_price, car_status_type,
    fuel_type, transmission_type, body_type, color, kilometer, doors, seats,
    image_url, thumbnail_url
) VALUES (
    NOW(), NOW(), FALSE, 0,
    '34TSL002', '5YJ3E1EA5KF000007', 'Tesla', 'Model 3', 2023,
    180.00, 'USD', 0, 'AVAILABLE',
    'Electric', 'Automatic', 'Sedan', 'White', 12000, 4, 5,
    'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&q=80&w=1200',
    'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&q=80&w=400'
) ON CONFLICT (license_plate) DO UPDATE SET
    brand = EXCLUDED.brand,
    model = EXCLUDED.model,
    production_year = EXCLUDED.production_year,
    price = EXCLUDED.price,
    currency_type = EXCLUDED.currency_type,
    damage_price = EXCLUDED.damage_price,
    car_status_type = EXCLUDED.car_status_type,
    fuel_type = EXCLUDED.fuel_type,
    transmission_type = EXCLUDED.transmission_type,
    body_type = EXCLUDED.body_type,
    color = EXCLUDED.color,
    kilometer = EXCLUDED.kilometer,
    doors = EXCLUDED.doors,
    seats = EXCLUDED.seats,
    image_url = EXCLUDED.image_url,
    thumbnail_url = EXCLUDED.thumbnail_url,
    update_time = EXCLUDED.update_time;
