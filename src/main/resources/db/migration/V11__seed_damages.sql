-- =============================================================================
-- V11: Seed Damage Reports (10 reports across different severities/statuses)
-- =============================================================================
-- Status: REPORTED, UNDER_ASSESSMENT, ASSESSED, CHARGED, DISPUTED, RESOLVED
-- Severity: MINOR, MODERATE, SEVERE
-- =============================================================================

-- -----------------------------------------------------------------------------
-- RESOLVED Damages (3) - Historical, fully processed
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'esra.dogan@hotmail.com' AND car_license_plate = '06VLV010' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010',
    '2025-08-05', '2025-08-12',
    'esra.dogan@hotmail.com', 'Esra Doğan',
    (SELECT id FROM gallery.users WHERE email = 'esra.dogan@hotmail.com'),
    'Minor scratch on rear bumper, approximately 10cm long. Surface level damage only.',
    'Rear bumper, left side',
    'MINOR', 'SCRATCH', 'RESOLVED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '89 days',
    150.00, 75.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '88 days',
    'Surface scratch only, no structural damage. Touch-up paint sufficient.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '85 days',
    'Customer paid liability. Repair completed successfully.'
);

INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'murat.aydin@outlook.com' AND car_license_plate = '35JKL789' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '35JKL789'),
    'Audi', 'A4', '35JKL789',
    '2025-06-15', '2025-06-22',
    'murat.aydin@outlook.com', 'Murat Aydın',
    (SELECT id FROM gallery.users WHERE email = 'murat.aydin@outlook.com'),
    'Small dent on driver side door. Customer reported hitting shopping cart.',
    'Driver side door, middle section',
    'MINOR', 'DENT', 'RESOLVED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '158 days',
    280.00, 140.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '155 days',
    'Paintless dent repair possible. Standard liability applies.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '150 days',
    'Repair completed. Customer paid via credit card on file.'
);

INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'gokhan.erdogan@gmail.com' AND car_license_plate = '06VLV010' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '06VLV010'),
    'Volvo', 'XC60', '06VLV010',
    '2025-05-05', '2025-05-10',
    'gokhan.erdogan@gmail.com', 'Gökhan Erdoğan',
    (SELECT id FROM gallery.users WHERE email = 'gokhan.erdogan@gmail.com'),
    'Coffee stain on passenger seat. Customer admitted spill during return inspection.',
    'Passenger seat, center',
    'MINOR', 'INTERIOR', 'RESOLVED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '218 days',
    120.00, 60.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '215 days',
    'Professional cleaning required. Stain removed successfully.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '210 days',
    'Cleaning completed. Customer charged cleaning fee.'
);

-- -----------------------------------------------------------------------------
-- CHARGED Damages (2) - Payment collected, awaiting repair
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'okan.tekin@outlook.com' AND car_license_plate = '35SEA013' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '35SEA013'),
    'Seat', 'Leon', '35SEA013',
    '2025-09-20', '2025-09-25',
    'okan.tekin@outlook.com', 'Okan Tekin',
    (SELECT id FROM gallery.users WHERE email = 'okan.tekin@outlook.com'),
    'Front bumper cracked after minor collision. Customer reported hitting curb while parking.',
    'Front bumper, center',
    'MODERATE', 'DENT', 'CHARGED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '43 days',
    650.00, 325.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '42 days',
    'Bumper requires replacement. Paint matching needed.',
    'COMPLETED'
);

INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'mehmet.demir@gmail.com' AND car_license_plate = '16XYZ321' AND status = 'RETURNED' LIMIT 1),
    (SELECT id FROM gallery.car WHERE license_plate = '16XYZ321'),
    'Honda', 'Civic', '16XYZ321',
    '2025-10-15', '2025-10-20',
    'mehmet.demir@gmail.com', 'Mehmet Demir',
    (SELECT id FROM gallery.users WHERE email = 'mehmet.demir@gmail.com'),
    'Tire puncture and rim damage from pothole. Spare tire used during rental.',
    'Front right wheel',
    'MODERATE', 'TIRE', 'CHARGED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '28 days',
    480.00, 240.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '27 days',
    'New tire and rim required. Road hazard, standard liability applies.',
    'COMPLETED'
);

-- -----------------------------------------------------------------------------
-- ASSESSED Damage (1) - Awaiting customer payment
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'serkan.yilmaz@yahoo.com' AND car_license_plate = '34NSN012' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '34NSN012'),
    'Nissan', 'Qashqai', '34NSN012',
    '2025-08-20', '2025-08-25',
    'serkan.yilmaz@yahoo.com', 'Serkan Yılmaz',
    (SELECT id FROM gallery.users WHERE email = 'serkan.yilmaz@yahoo.com'),
    'Side mirror cracked. Unknown cause, discovered during return inspection.',
    'Driver side mirror',
    'MODERATE', 'GLASS', 'ASSESSED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '9 days',
    350.00, 175.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '8 days',
    'Mirror housing and glass replacement needed. Customer notified for payment.'
);

-- -----------------------------------------------------------------------------
-- DISPUTED Damage (1) - Customer disputed the charge
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
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
    (SELECT id FROM gallery.rentals WHERE user_email = 'yusuf.karaca@gmail.com' AND car_license_plate = '34MRC001' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '34MRC001'),
    'Mercedes', 'C200', '34MRC001',
    '2025-07-01', '2025-07-11',
    'yusuf.karaca@gmail.com', 'Yusuf Karaca',
    (SELECT id FROM gallery.users WHERE email = 'yusuf.karaca@gmail.com'),
    'Deep scratch on hood. Appears to be from key or sharp object.',
    'Hood, center',
    'SEVERE', 'SCRATCH', 'DISPUTED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '19 days',
    1200.00, 600.00,
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '18 days',
    'Deep scratch requiring professional paint repair. Multiple panels affected.',
    (SELECT id FROM gallery.users WHERE email = 'yusuf.karaca@gmail.com'),
    NOW() - INTERVAL '16 days',
    'Customer claims damage was pre-existing',
    'Customer states the scratch was present when vehicle was picked up but not documented in initial inspection.'
);

-- -----------------------------------------------------------------------------
-- UNDER_ASSESSMENT Damage (1) - Currently being evaluated
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'pinar.sen@gmail.com' AND car_license_plate = '06DEF456' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '06DEF456'),
    'BMW', '320i', '06DEF456',
    '2025-09-10', '2025-09-15',
    'pinar.sen@gmail.com', 'Pınar Şen',
    (SELECT id FROM gallery.users WHERE email = 'pinar.sen@gmail.com'),
    'Unusual engine noise reported. Customer mentioned rough idle during the rental period.',
    'Engine compartment',
    'SEVERE', 'MECHANICAL', 'UNDER_ASSESSMENT',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '2 days'
);

-- -----------------------------------------------------------------------------
-- REPORTED Damages (2) - Newly reported, awaiting assessment
-- -----------------------------------------------------------------------------
INSERT INTO gallery.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '6 hours', NOW() - INTERVAL '6 hours', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ahmet.yilmaz@gmail.com' AND car_license_plate = '34HYN006' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '34HYN006'),
    'Hyundai', 'Tucson', '34HYN006',
    '2025-07-15', '2025-07-22',
    'ahmet.yilmaz@gmail.com', 'Ahmet Yılmaz',
    (SELECT id FROM gallery.users WHERE email = 'ahmet.yilmaz@gmail.com'),
    'Small chip in windshield. Customer noticed but did not report during rental.',
    'Windshield, passenger side',
    'MINOR', 'GLASS', 'REPORTED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '6 hours'
);

INSERT INTO gallery.damage_reports (
    create_time, update_time, is_deleted, version,
    rental_id, car_id,
    car_brand, car_model, car_license_plate,
    rental_start_date, rental_end_date,
    customer_email, customer_full_name, customer_user_id,
    description, damage_location, severity, category, status,
    reported_by, reported_at
) VALUES (
    NOW() - INTERVAL '2 hours', NOW() - INTERVAL '2 hours', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'zeynep.kaya@gmail.com' AND car_license_plate = '06OPL014' AND status = 'RETURNED'),
    (SELECT id FROM gallery.car WHERE license_plate = '06OPL014'),
    'Opel', 'Astra', '06OPL014',
    '2025-07-25', '2025-07-28',
    'zeynep.kaya@gmail.com', 'Zeynep Kaya',
    (SELECT id FROM gallery.users WHERE email = 'zeynep.kaya@gmail.com'),
    'Cigarette burn on rear seat fabric. Customer claims pre-existing.',
    'Rear seat, left side',
    'MINOR', 'INTERIOR', 'REPORTED',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '2 hours'
);
