INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    resolved_by, resolved_at, resolution_notes
) VALUES (
    NOW() - INTERVAL '90 days', NOW() - INTERVAL '85 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'esra.dogan@example.com' AND car_license_plate = '06VLV010' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010',
    '2025-08-05', '2025-08-12',
    'esra.dogan@example.com', 'Esra Doğan',
    (SELECT id FROM car_rental.users WHERE email = 'esra.dogan@example.com'),
    'Minor scratch on rear bumper, approximately 10cm long. Surface level damage only.',
    'Rear bumper, left side',
    'MINOR', 'SCRATCH', 'RESOLVED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '89 days',
    150.00, 75.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '88 days',
    'Surface scratch only, no structural damage. Touch-up paint sufficient.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '85 days',
    'Customer paid liability. Repair completed successfully.'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    resolved_by, resolved_at, resolution_notes
) VALUES (
    NOW() - INTERVAL '160 days', NOW() - INTERVAL '150 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'murat.aydin@example.com' AND car_license_plate = '35JKL789' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '35JKL789'),
    'Audi', 'A4', '35JKL789',
    '2025-06-15', '2025-06-22',
    'murat.aydin@example.com', 'Murat Aydın',
    (SELECT id FROM car_rental.users WHERE email = 'murat.aydin@example.com'),
    'Small dent on driver side door. Customer reported hitting shopping cart.',
    'Driver side door, middle section',
    'MINOR', 'DENT', 'RESOLVED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '158 days',
    280.00, 140.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '155 days',
    'Paintless dent repair possible. Standard liability applies.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '150 days',
    'Repair completed. Customer paid via credit card on file.'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    resolved_by, resolved_at, resolution_notes
) VALUES (
    NOW() - INTERVAL '220 days', NOW() - INTERVAL '210 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'gokhan.erdogan@example.com' AND car_license_plate = '06VLV010' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010',
    '2025-05-05', '2025-05-10',
    'gokhan.erdogan@example.com', 'Gökhan Erdoğan',
    (SELECT id FROM car_rental.users WHERE email = 'gokhan.erdogan@example.com'),
    'Coffee stain on passenger seat. Customer admitted spill during return inspection.',
    'Passenger seat, center',
    'MINOR', 'INTERIOR', 'RESOLVED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '218 days',
    120.00, 60.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '215 days',
    'Professional cleaning required. Stain removed successfully.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '210 days',
    'Cleaning completed. Customer charged cleaning fee.'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    payment_status
) VALUES (
    NOW() - INTERVAL '45 days', NOW() - INTERVAL '40 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'okan.tekin@example.com' AND car_license_plate = '35SEA013' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '35SEA013'),
    'Seat', 'Leon', '35SEA013',
    '2025-09-20', '2025-09-25',
    'okan.tekin@example.com', 'Okan Tekin',
    (SELECT id FROM car_rental.users WHERE email = 'okan.tekin@example.com'),
    'Front bumper cracked after minor collision. Customer reported hitting curb while parking.',
    'Front bumper, center',
    'MODERATE', 'DENT', 'CHARGED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '43 days',
    650.00, 325.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '42 days',
    'Bumper requires replacement. Paint matching needed.',
    'COMPLETED'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    payment_status
) VALUES (
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '25 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'mehmet.demir@example.com' AND car_license_plate = '16XYZ321' AND status = 'RETURNED' LIMIT 1),
    (SELECT id FROM car_rental.car WHERE license_plate = '16XYZ321'),
    'Honda', 'Civic', '16XYZ321',
    '2025-10-15', '2025-10-20',
    'mehmet.demir@example.com', 'Mehmet Demir',
    (SELECT id FROM car_rental.users WHERE email = 'mehmet.demir@example.com'),
    'Tire puncture and rim damage from pothole. Spare tire used during rental.',
    'Front right wheel',
    'MODERATE', 'TIRE', 'CHARGED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '28 days',
    480.00, 240.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '27 days',
    'New tire and rim required. Road hazard, standard liability applies.',
    'COMPLETED'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes
) VALUES (
    NOW() - INTERVAL '10 days', NOW() - INTERVAL '8 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'serkan.yilmaz@example.com' AND car_license_plate = '34NSN012' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '34NSN012'),
    'Nissan', 'Qashqai', '34NSN012',
    '2025-08-20', '2025-08-25',
    'serkan.yilmaz@example.com', 'Serkan Yılmaz',
    (SELECT id FROM car_rental.users WHERE email = 'serkan.yilmaz@example.com'),
    'Side mirror cracked. Unknown cause, discovered during return inspection.',
    'Driver side mirror',
    'MODERATE', 'GLASS', 'ASSESSED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '9 days',
    350.00, 175.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '8 days',
    'Mirror housing and glass replacement needed. Customer notified for payment.'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at,
    repair_cost_estimate, customer_liability,
    assessed_by, assessed_at, assessment_notes,
    disputed_by, disputed_at, dispute_reason, dispute_comments
) VALUES (
    NOW() - INTERVAL '20 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'yusuf.karaca@example.com' AND car_license_plate = '34MRC001' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '34MRC001'),
    'Mercedes', 'C200', '34MRC001',
    '2025-07-01', '2025-07-11',
    'yusuf.karaca@example.com', 'Yusuf Karaca',
    (SELECT id FROM car_rental.users WHERE email = 'yusuf.karaca@example.com'),
    'Deep scratch on hood. Appears to be from key or sharp object.',
    'Hood, center',
    'SEVERE', 'SCRATCH', 'DISPUTED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '19 days',
    1200.00, 600.00,
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '18 days',
    'Deep scratch requiring professional paint repair. Multiple panels affected.',
    (SELECT id FROM car_rental.users WHERE email = 'yusuf.karaca@example.com'),
    NOW() - INTERVAL '16 days',
    'Customer claims damage was pre-existing',
    'Customer states the scratch was present when vehicle was picked up but not documented in initial inspection.'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'pinar.sen@example.com' AND car_license_plate = '06DEF456' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '06DEF456'),
    'BMW', '320i', '06DEF456',
    '2025-09-10', '2025-09-15',
    'pinar.sen@example.com', 'Pınar Şen',
    (SELECT id FROM car_rental.users WHERE email = 'pinar.sen@example.com'),
    'Unusual engine noise reported. Customer mentioned rough idle during the rental period.',
    'Engine compartment',
    'SEVERE', 'MECHANICAL', 'UNDER_ASSESSMENT',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '2 days'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ahmet.yilmaz@example.com' AND car_license_plate = '34HYN006' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '34HYN006'),
    'Hyundai', 'Tucson', '34HYN006',
    '2025-07-15', '2025-07-22',
    'ahmet.yilmaz@example.com', 'Ahmet Yılmaz',
    (SELECT id FROM car_rental.users WHERE email = 'ahmet.yilmaz@example.com'),
    'Small chip in windshield. Customer noticed but did not report during rental.',
    'Windshield, passenger side',
    'MINOR', 'GLASS', 'REPORTED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '6 hours'
);

INSERT INTO car_rental.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'zeynep.kaya@example.com' AND car_license_plate = '06OPL014' AND status = 'RETURNED'),
    (SELECT id FROM car_rental.car WHERE license_plate = '06OPL014'),
    'Opel', 'Astra', '06OPL014',
    '2025-07-25', '2025-07-28',
    'zeynep.kaya@example.com', 'Zeynep Kaya',
    (SELECT id FROM car_rental.users WHERE email = 'zeynep.kaya@example.com'),
    'Cigarette burn on rear seat fabric. Customer claims pre-existing.',
    'Rear seat, left side',
    'MINOR', 'INTERIOR', 'REPORTED',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '2 hours'
);
