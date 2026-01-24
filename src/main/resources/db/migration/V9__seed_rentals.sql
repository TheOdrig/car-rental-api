-- =============================================================================
-- V9: Seed Rentals (35 rentals across 12 months for revenue chart)
-- =============================================================================
-- Status Distribution:
--   RETURNED: 25 (completed rentals - revenue data)
--   IN_USE: 3 (currently active)
--   CONFIRMED: 2 (upcoming)
--   REQUESTED: 3 (pending approval)
--   CANCELLED: 2 (cancelled examples)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RETURNED Rentals (25 rentals - Last 12 months for revenue chart)
-- Note: These generate the monthly revenue data
-- -----------------------------------------------------------------------------

-- January 2025 (2 rentals, ~$500 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '365 days', NOW() - INTERVAL '360 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ahmet.yilmaz@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34ABC123'),
    'Volkswagen', 'Golf', '34ABC123', 'https://images.unsplash.com/photo-1718629879998-ee8cfc09df39?auto=format&fit=crop&q=80&w=400',
    'ahmet.yilmaz@gmail.com', 'Ahmet Yılmaz',
    '2025-01-10', '2025-01-15', 5,
    'USD', 35.00, 175.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '355 days', NOW() - INTERVAL '348 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'zeynep.kaya@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16RNL005'),
    'Renault', 'Clio', '16RNL005', 'https://images.unsplash.com/photo-1594502225401-a9eab8b405dd?auto=format&fit=crop&q=80&w=400',
    'zeynep.kaya@gmail.com', 'Zeynep Kaya',
    '2025-01-18', '2025-01-25', 7,
    'USD', 40.00, 280.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- February 2025 (2 rentals, ~$600 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '330 days', NOW() - INTERVAL '325 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'mehmet.demir@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16XYZ321'),
    'Honda', 'Civic', '16XYZ321', 'https://images.unsplash.com/photo-1605515321331-46a63d31758c?auto=format&fit=crop&q=80&w=400',
    'mehmet.demir@gmail.com', 'Mehmet Demir',
    '2025-02-05', '2025-02-10', 5,
    'USD', 45.00, 225.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '315 days', NOW() - INTERVAL '307 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ayse.ozturk@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34HYN006'),
    'Hyundai', 'Tucson', '34HYN006', 'https://images.unsplash.com/photo-1705624843697-4461f9dce482?auto=format&fit=crop&q=80&w=400',
    'ayse.ozturk@hotmail.com', 'Ayşe Öztürk',
    '2025-02-15', '2025-02-23', 8,
    'USD', 85.00, 680.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- March 2025 (3 rentals, ~$900 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '300 days', NOW() - INTERVAL '295 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'can.arslan@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06DEF456'),
    'BMW', '320i', '06DEF456', 'https://images.unsplash.com/photo-1639056067266-43a821cf0a1f?auto=format&fit=crop&q=80&w=400',
    'can.arslan@outlook.com', 'Can Arslan',
    '2025-03-01', '2025-03-06', 5,
    'USD', 95.00, 475.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '285 days', NOW() - INTERVAL '282 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'elif.sahin@yahoo.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16CTR015'),
    'Citroen', 'C3', '16CTR015', 'https://images.unsplash.com/photo-1609030429269-ca5b7a409310?auto=format&fit=crop&q=80&w=400',
    'elif.sahin@yahoo.com', 'Elif Şahin',
    '2025-03-15', '2025-03-18', 3,
    'USD', 38.00, 114.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '275 days', NOW() - INTERVAL '271 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'burak.celik@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '01MNO654'),
    'Toyota', 'Corolla', '01MNO654', 'https://www.thedrive.com/wp-content/uploads/2023/05/02/corollahatch-1-scaled.jpg?w=400',
    'burak.celik@gmail.com', 'Burak Çelik',
    '2025-03-22', '2025-03-26', 4,
    'USD', 65.00, 260.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- April 2025 (3 rentals, ~$1100 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '260 days', NOW() - INTERVAL '253 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'selin.yildiz@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34MRC001'),
    'Mercedes', 'C200', '34MRC001', 'https://www.sixt.com.tr/storage/cache/a4831ea1285880c2536797abe33a35a517ddeacf.webp',
    'selin.yildiz@gmail.com', 'Selin Yıldız',
    '2025-04-05', '2025-04-12', 7,
    'USD', 120.00, 840.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '245 days', NOW() - INTERVAL '242 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'emre.koc@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35FIA004'),
    'Fiat', 'Egea', '35FIA004', 'https://www.log.com.tr/wp-content/uploads/2023/01/2025-fiat-egea-tipo-suv-benzeri-bir-formda-olacak-copy-1000x562.jpg',
    'emre.koc@hotmail.com', 'Emre Koç',
    '2025-04-18', '2025-04-21', 3,
    'USD', 42.00, 126.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '235 days', NOW() - INTERVAL '233 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'deniz.aksoy@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35MZD008'),
    'Mazda', '3', '35MZD008', 'https://images.unsplash.com/photo-1643142311721-c36cd233fb95?auto=format&fit=crop&q=80&w=400',
    'deniz.aksoy@outlook.com', 'Deniz Aksoy',
    '2025-04-25', '2025-04-27', 2,
    'USD', 55.00, 110.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- May 2025 (3 rentals, ~$1300 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '220 days', NOW() - INTERVAL '215 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'gokhan.erdogan@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010', 'https://images.unsplash.com/photo-1629897046038-371765238f26?auto=format&fit=crop&q=80&w=400',
    'gokhan.erdogan@gmail.com', 'Gökhan Erdoğan',
    '2025-05-05', '2025-05-10', 5,
    'USD', 145.00, 725.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '205 days', NOW() - INTERVAL '200 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'merve.kilic@yahoo.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06KIA007'),
    'Kia', 'Sportage', '06KIA007', 'https://images.unsplash.com/photo-1688893287585-b1bc00e608fc?auto=format&fit=crop&q=80&w=400',
    'merve.kilic@yahoo.com', 'Merve Kılıç',
    '2025-05-15', '2025-05-20', 5,
    'USD', 80.00, 400.00,
    'RETURNED', 'GRACE_PERIOD',
    '2025-05-20 14:30:00', 2, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '190 days', NOW() - INTERVAL '187 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ali.polat@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16SKD011'),
    'Skoda', 'Octavia', '16SKD011', 'https://images.unsplash.com/photo-1673822317394-6fd502a324e6?auto=format&fit=crop&q=80&w=400',
    'ali.polat@gmail.com', 'Ali Polat',
    '2025-05-25', '2025-05-28', 3,
    'USD', 60.00, 180.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- June 2025 (3 rentals, ~$1800 revenue - summer peak starts)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '175 days', NOW() - INTERVAL '165 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ipek.ozdemir@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34TSL002'),
    'Tesla', 'Model 3', '34TSL002', 'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&q=80&w=400',
    'ipek.ozdemir@hotmail.com', 'İpek Özdemir',
    '2025-06-01', '2025-06-11', 10,
    'USD', 180.00, 1800.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '160 days', NOW() - INTERVAL '153 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'murat.aydin@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35JKL789'),
    'Audi', 'A4', '35JKL789', 'https://images.unsplash.com/photo-1710011115921-7e67a8e4b483?auto=format&fit=crop&q=80&w=400',
    'murat.aydin@outlook.com', 'Murat Aydın',
    '2025-06-15', '2025-06-22', 7,
    'USD', 90.00, 630.00,
    'RETURNED', 'LATE',
    '2025-06-23 16:00:00', 16, 90.00, TRUE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '145 days', NOW() - INTERVAL '140 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ceren.ozer@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06FRD003'),
    'Ford', 'Focus', '06FRD003', 'https://images.unsplash.com/photo-1708849894321-2c9bc515df0e?auto=format&fit=crop&q=80&w=400',
    'ceren.ozer@gmail.com', 'Ceren Özer',
    '2025-06-25', '2025-06-30', 5,
    'USD', 48.00, 240.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- July 2025 (3 rentals, ~$2000 revenue - peak summer)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '130 days', NOW() - INTERVAL '120 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'yusuf.karaca@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34MRC001'),
    'Mercedes', 'C200', '34MRC001', 'https://www.sixt.com.tr/storage/cache/a4831ea1285880c2536797abe33a35a517ddeacf.webp',
    'yusuf.karaca@gmail.com', 'Yusuf Karaca',
    '2025-07-01', '2025-07-11', 10,
    'USD', 120.00, 1200.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '115 days', NOW() - INTERVAL '108 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ahmet.yilmaz@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34HYN006'),
    'Hyundai', 'Tucson', '34HYN006', 'https://images.unsplash.com/photo-1705624843697-4461f9dce482?auto=format&fit=crop&q=80&w=400',
    'ahmet.yilmaz@gmail.com', 'Ahmet Yılmaz',
    '2025-07-15', '2025-07-22', 7,
    'USD', 85.00, 595.00,
    'RETURNED', 'GRACE_PERIOD',
    '2025-07-22 11:00:00', 3, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '100 days', NOW() - INTERVAL '97 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'zeynep.kaya@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06OPL014'),
    'Opel', 'Astra', '06OPL014', 'https://images.unsplash.com/photo-1582639510494-c80b5de9f148?auto=format&fit=crop&q=80&w=400',
    'zeynep.kaya@gmail.com', 'Zeynep Kaya',
    '2025-07-25', '2025-07-28', 3,
    'USD', 42.00, 126.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- August 2025 (2 rentals, ~$1500 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '90 days', NOW() - INTERVAL '83 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'esra.dogan@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010', 'https://images.unsplash.com/photo-1629897046038-371765238f26?auto=format&fit=crop&q=80&w=400',
    'esra.dogan@hotmail.com', 'Esra Doğan',
    '2025-08-05', '2025-08-12', 7,
    'USD', 145.00, 1015.00,
    'RETURNED', 'LATE',
    '2025-08-13 10:00:00', 10, 145.00, TRUE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '75 days', NOW() - INTERVAL '70 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'serkan.yilmaz@yahoo.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34NSN012'),
    'Nissan', 'Qashqai', '34NSN012', 'https://images.unsplash.com/photo-1538940714252-fc70779afa46?auto=format&fit=crop&q=80&w=400',
    'serkan.yilmaz@yahoo.com', 'Serkan Yılmaz',
    '2025-08-20', '2025-08-25', 5,
    'USD', 75.00, 375.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- September 2025 (2 rentals, ~$800 revenue)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '60 days', NOW() - INTERVAL '55 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'pinar.sen@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06DEF456'),
    'BMW', '320i', '06DEF456', 'https://images.unsplash.com/photo-1639056067266-43a821cf0a1f?auto=format&fit=crop&q=80&w=400',
    'pinar.sen@gmail.com', 'Pınar Şen',
    '2025-09-10', '2025-09-15', 5,
    'USD', 95.00, 475.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '45 days', NOW() - INTERVAL '40 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'okan.tekin@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35SEA013'),
    'Seat', 'Leon', '35SEA013', 'https://cdn3.focus.bg/autodata/i/seat/leon/leon-cupra-ii/large/c34323f28221796d10f7df07e042d14f.jpg',
    'okan.tekin@outlook.com', 'Okan Tekin',
    '2025-09-20', '2025-09-25', 5,
    'USD', 45.00, 225.00,
    'RETURNED', 'SEVERELY_LATE',
    '2025-09-27 20:00:00', 44, 180.00, FALSE
);

-- October-December 2025 + January 2026 (remaining rentals)
-- October 2025 (1 rental, ~$400)
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    actual_return_time, late_hours, penalty_amount, penalty_paid
) VALUES (
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '25 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'mehmet.demir@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16XYZ321'),
    'Honda', 'Civic', '16XYZ321', 'https://images.unsplash.com/photo-1605515321331-46a63d31758c?auto=format&fit=crop&q=80&w=400',
    'mehmet.demir@gmail.com', 'Mehmet Demir',
    '2025-10-15', '2025-10-20', 5,
    'USD', 45.00, 225.00,
    'RETURNED', 'ON_TIME',
    NULL, NULL, NULL, FALSE
);

-- -----------------------------------------------------------------------------
-- IN_USE Rentals (3 currently active)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '3 days', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'can.arslan@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35FIA004'),
    'Fiat', 'Egea', '35FIA004', 'https://www.log.com.tr/wp-content/uploads/2023/01/2025-fiat-egea-tipo-suv-benzeri-bir-formda-olacak-copy-1000x562.jpg',
    'can.arslan@outlook.com', 'Can Arslan',
    CURRENT_DATE - INTERVAL '3 days', CURRENT_DATE + INTERVAL '2 days', 5,
    'USD', 42.00, 210.00,
    'IN_USE', NULL
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '5 days', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'elif.sahin@yahoo.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34NSN012'),
    'Nissan', 'Qashqai', '34NSN012', 'https://images.unsplash.com/photo-1538940714252-fc70779afa46?auto=format&fit=crop&q=80&w=400',
    'elif.sahin@yahoo.com', 'Elif Şahin',
    CURRENT_DATE - INTERVAL '5 days', CURRENT_DATE + INTERVAL '1 day', 6,
    'USD', 75.00, 450.00,
    'IN_USE', NULL
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '1 day', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'burak.celik@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34TSL002'),
    'Tesla', 'Model 3', '34TSL002', 'https://images.unsplash.com/photo-1560958089-b8a1929cea89?auto=format&fit=crop&q=80&w=400',
    'burak.celik@gmail.com', 'Burak Çelik',
    CURRENT_DATE - INTERVAL '1 day', CURRENT_DATE + INTERVAL '4 days', 5,
    'USD', 180.00, 900.00,
    'IN_USE', NULL
);

-- -----------------------------------------------------------------------------
-- CONFIRMED Rentals (2 upcoming)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '2 days', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'selin.yildiz@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06KIA007'),
    'Kia', 'Sportage', '06KIA007', 'https://images.unsplash.com/photo-1688893287585-b1bc00e608fc?auto=format&fit=crop&q=80&w=400',
    'selin.yildiz@gmail.com', 'Selin Yıldız',
    CURRENT_DATE + INTERVAL '3 days', CURRENT_DATE + INTERVAL '10 days', 7,
    'USD', 80.00, 560.00,
    'CONFIRMED', NULL
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '1 day', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'emre.koc@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '34MRC001'),
    'Mercedes', 'C200', '34MRC001', 'https://www.sixt.com.tr/storage/cache/a4831ea1285880c2536797abe33a35a517ddeacf.webp',
    'emre.koc@hotmail.com', 'Emre Koç',
    CURRENT_DATE + INTERVAL '7 days', CURRENT_DATE + INTERVAL '12 days', 5,
    'USD', 120.00, 600.00,
    'CONFIRMED', NULL
);

-- -----------------------------------------------------------------------------
-- REQUESTED Rentals (3 pending approval)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '2 hours', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'deniz.aksoy@outlook.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06DEF456'),
    'BMW', '320i', '06DEF456', 'https://images.unsplash.com/photo-1639056067266-43a821cf0a1f?auto=format&fit=crop&q=80&w=400',
    'deniz.aksoy@outlook.com', 'Deniz Aksoy',
    CURRENT_DATE + INTERVAL '5 days', CURRENT_DATE + INTERVAL '8 days', 3,
    'USD', 95.00, 285.00,
    'REQUESTED', NULL
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '4 hours', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'gokhan.erdogan@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010', 'https://images.unsplash.com/photo-1629897046038-371765238f26?auto=format&fit=crop&q=80&w=400',
    'gokhan.erdogan@gmail.com', 'Gökhan Erdoğan',
    CURRENT_DATE + INTERVAL '10 days', CURRENT_DATE + INTERVAL '17 days', 7,
    'USD', 145.00, 1015.00,
    'REQUESTED', NULL
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status
) VALUES (
    NOW() - INTERVAL '1 hour', NOW(), FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'merve.kilic@yahoo.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '35MZD008'),
    'Mazda', '3', '35MZD008', 'https://images.unsplash.com/photo-1643142311721-c36cd233fb95?auto=format&fit=crop&q=80&w=400',
    'merve.kilic@yahoo.com', 'Merve Kılıç',
    CURRENT_DATE + INTERVAL '2 days', CURRENT_DATE + INTERVAL '4 days', 2,
    'USD', 55.00, 110.00,
    'REQUESTED', NULL
);

-- -----------------------------------------------------------------------------
-- CANCELLED Rentals (2)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    cancellation_reason
) VALUES (
    NOW() - INTERVAL '20 days', NOW() - INTERVAL '18 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ali.polat@gmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '16RNL005'),
    'Renault', 'Clio', '16RNL005', 'https://images.unsplash.com/photo-1594502225401-a9eab8b405dd?auto=format&fit=crop&q=80&w=400',
    'ali.polat@gmail.com', 'Ali Polat',
    '2025-10-28', '2025-11-02', 5,
    'USD', 40.00, 200.00,
    'CANCELLED', NULL,
    'Customer requested cancellation - travel plans changed'
);

INSERT INTO gallery.rentals (
    create_time, update_time, is_deleted, version,
    user_id, car_id,
    car_brand, car_model, car_license_plate, car_thumbnail_url,
    user_email, user_full_name,
    start_date, end_date, days,
    currency, daily_price, total_price,
    status, late_return_status,
    cancellation_reason
) VALUES (
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '14 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ipek.ozdemir@hotmail.com'),
    (SELECT id FROM gallery.car WHERE license_plate = '01MNO654'),
    'Toyota', 'Corolla', '01MNO654', 'https://www.thedrive.com/wp-content/uploads/2023/05/02/corollahatch-1-scaled.jpg?w=400',
    'ipek.ozdemir@hotmail.com', 'İpek Özdemir',
    '2025-11-01', '2025-11-05', 4,
    'USD', 65.00, 260.00,
    'CANCELLED', NULL,
    'Vehicle not available - double booking error'
);
