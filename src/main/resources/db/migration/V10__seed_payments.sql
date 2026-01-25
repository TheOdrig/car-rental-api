-- =============================================================================
-- V10: Seed Payments (Revenue data for dashboard charts)
-- =============================================================================
-- CRITICAL: payment.create_time determines chart position
-- Status: CAPTURED = counted as revenue
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CAPTURED Payments (for RETURNED + IN_USE rentals)
-- These are the payments that show up in revenue chart
-- Note: create_time must match when payment was actually processed
-- -----------------------------------------------------------------------------

-- January 2025 Payments (~$455)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-01-10 10:00:00'::timestamp, '2025-01-10 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ahmet.yilmaz@gmail.com' AND car_license_plate = '34ABC123' AND start_date = '2025-01-10'),
    'ahmet.yilmaz@gmail.com', '34ABC123',
    175.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jan_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-01-18 09:30:00'::timestamp, '2025-01-18 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'zeynep.kaya@gmail.com' AND car_license_plate = '16RNL005' AND start_date = '2025-01-18'),
    'zeynep.kaya@gmail.com', '16RNL005',
    280.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jan_002'
);

-- February 2025 Payments (~$905)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-02-05 11:00:00'::timestamp, '2025-02-05 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'mehmet.demir@gmail.com' AND car_license_plate = '16XYZ321' AND start_date = '2025-02-05'),
    'mehmet.demir@gmail.com', '16XYZ321',
    225.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_feb_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-02-15 14:00:00'::timestamp, '2025-02-15 14:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ayse.ozturk@hotmail.com' AND car_license_plate = '34HYN006' AND start_date = '2025-02-15'),
    'ayse.ozturk@hotmail.com', '34HYN006',
    680.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_feb_002'
);

-- March 2025 Payments (~$849)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-03-01 10:30:00'::timestamp, '2025-03-01 10:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'can.arslan@outlook.com' AND car_license_plate = '06DEF456' AND start_date = '2025-03-01'),
    'can.arslan@outlook.com', '06DEF456',
    475.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-03-15 15:00:00'::timestamp, '2025-03-15 15:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'elif.sahin@yahoo.com' AND car_license_plate = '16CTR015' AND start_date = '2025-03-15'),
    'elif.sahin@yahoo.com', '16CTR015',
    114.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-03-22 09:00:00'::timestamp, '2025-03-22 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'burak.celik@gmail.com' AND car_license_plate = '01MNO654' AND start_date = '2025-03-22'),
    'burak.celik@gmail.com', '01MNO654',
    260.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_003'
);

-- April 2025 Payments (~$1076)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-04-05 10:00:00'::timestamp, '2025-04-05 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'selin.yildiz@gmail.com' AND car_license_plate = '34MRC001' AND start_date = '2025-04-05'),
    'selin.yildiz@gmail.com', '34MRC001',
    840.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-04-18 11:30:00'::timestamp, '2025-04-18 11:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'emre.koc@hotmail.com' AND car_license_plate = '35FIA004' AND start_date = '2025-04-18'),
    'emre.koc@hotmail.com', '35FIA004',
    126.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-04-25 14:00:00'::timestamp, '2025-04-25 14:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'deniz.aksoy@outlook.com' AND car_license_plate = '35MZD008' AND start_date = '2025-04-25'),
    'deniz.aksoy@outlook.com', '35MZD008',
    110.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_003'
);

-- May 2025 Payments (~$1305)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-05-05 09:00:00'::timestamp, '2025-05-05 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'gokhan.erdogan@gmail.com' AND car_license_plate = '06VLV010' AND start_date = '2025-05-05'),
    'gokhan.erdogan@gmail.com', '06VLV010',
    725.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-05-15 10:30:00'::timestamp, '2025-05-15 10:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'merve.kilic@yahoo.com' AND car_license_plate = '06KIA007' AND start_date = '2025-05-15'),
    'merve.kilic@yahoo.com', '06KIA007',
    400.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-05-25 11:00:00'::timestamp, '2025-05-25 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ali.polat@gmail.com' AND car_license_plate = '16SKD011' AND start_date = '2025-05-25'),
    'ali.polat@gmail.com', '16SKD011',
    180.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_003'
);

-- June 2025 Payments (~$2760 - summer peak)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-06-01 08:30:00'::timestamp, '2025-06-01 08:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ipek.ozdemir@hotmail.com' AND car_license_plate = '34TSL002' AND start_date = '2025-06-01'),
    'ipek.ozdemir@hotmail.com', '34TSL002',
    1800.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-06-15 09:00:00'::timestamp, '2025-06-15 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'murat.aydin@outlook.com' AND car_license_plate = '35JKL789' AND start_date = '2025-06-15'),
    'murat.aydin@outlook.com', '35JKL789',
    720.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-06-25 10:00:00'::timestamp, '2025-06-25 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ceren.ozer@gmail.com' AND car_license_plate = '06FRD003' AND start_date = '2025-06-25'),
    'ceren.ozer@gmail.com', '06FRD003',
    240.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_003'
);

-- July 2025 Payments (~$1921 - peak summer)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-07-01 09:30:00'::timestamp, '2025-07-01 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'yusuf.karaca@gmail.com' AND car_license_plate = '34MRC001' AND start_date = '2025-07-01'),
    'yusuf.karaca@gmail.com', '34MRC001',
    1200.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-07-15 10:00:00'::timestamp, '2025-07-15 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ahmet.yilmaz@gmail.com' AND car_license_plate = '34HYN006' AND start_date = '2025-07-15'),
    'ahmet.yilmaz@gmail.com', '34HYN006',
    595.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-07-25 11:30:00'::timestamp, '2025-07-25 11:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'zeynep.kaya@gmail.com' AND car_license_plate = '06OPL014' AND start_date = '2025-07-25'),
    'zeynep.kaya@gmail.com', '06OPL014',
    126.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_003'
);

-- August 2025 Payments (~$1535)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-08-05 08:00:00'::timestamp, '2025-08-05 08:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'esra.dogan@hotmail.com' AND car_license_plate = '06VLV010' AND start_date = '2025-08-05'),
    'esra.dogan@hotmail.com', '06VLV010',
    1160.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_aug_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-08-20 09:30:00'::timestamp, '2025-08-20 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'serkan.yilmaz@yahoo.com' AND car_license_plate = '34NSN012' AND start_date = '2025-08-20'),
    'serkan.yilmaz@yahoo.com', '34NSN012',
    375.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_aug_002'
);

-- September 2025 Payments (~$880)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-09-10 10:00:00'::timestamp, '2025-09-10 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'pinar.sen@gmail.com' AND car_license_plate = '06DEF456' AND start_date = '2025-09-10'),
    'pinar.sen@gmail.com', '06DEF456',
    475.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_sep_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-09-20 11:00:00'::timestamp, '2025-09-20 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'okan.tekin@outlook.com' AND car_license_plate = '35SEA013' AND start_date = '2025-09-20'),
    'okan.tekin@outlook.com', '35SEA013',
    405.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_sep_002'
);

-- October 2025 Payment (~$225)
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    '2025-10-15 09:00:00'::timestamp, '2025-10-15 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'mehmet.demir@gmail.com' AND car_license_plate = '16XYZ321' AND start_date = '2025-10-15'),
    'mehmet.demir@gmail.com', '16XYZ321',
    225.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_oct_001'
);

-- -----------------------------------------------------------------------------
-- Payments for IN_USE rentals (processed when rental started)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'can.arslan@outlook.com' AND car_license_plate = '35FIA004' AND status = 'IN_USE'),
    'can.arslan@outlook.com', '35FIA004',
    210.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'elif.sahin@yahoo.com' AND car_license_plate = '34NSN012' AND status = 'IN_USE'),
    'elif.sahin@yahoo.com', '34NSN012',
    450.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_002'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'burak.celik@gmail.com' AND car_license_plate = '34TSL002' AND status = 'IN_USE'),
    'burak.celik@gmail.com', '34TSL002',
    900.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_003'
);

-- -----------------------------------------------------------------------------
-- PENDING Payments (for CONFIRMED rentals - not yet captured)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'selin.yildiz@gmail.com' AND car_license_plate = '06KIA007' AND status = 'CONFIRMED'),
    'selin.yildiz@gmail.com', '06KIA007',
    560.00, 'USD', 'PENDING',
    'card', 'pi_seed_pending_001'
);

INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'emre.koc@hotmail.com' AND car_license_plate = '34MRC001' AND status = 'CONFIRMED'),
    'emre.koc@hotmail.com', '34MRC001',
    600.00, 'USD', 'PENDING',
    'card', 'pi_seed_pending_002'
);

-- -----------------------------------------------------------------------------
-- REFUNDED Payment (for cancelled rental)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id,
    refunded_amount
) VALUES (
    NOW() - INTERVAL '20 days', NOW() - INTERVAL '18 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ali.polat@gmail.com' AND car_license_plate = '16RNL005' AND status = 'CANCELLED'),
    'ali.polat@gmail.com', '16RNL005',
    200.00, 'USD', 'REFUNDED',
    'card', 'pi_seed_refund_001',
    200.00
);

-- -----------------------------------------------------------------------------
-- FAILED Payment (test case)
-- -----------------------------------------------------------------------------
INSERT INTO gallery.payments (
    create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id,
    failure_reason
) VALUES (
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'ipek.ozdemir@hotmail.com' AND car_license_plate = '01MNO654' AND status = 'CANCELLED'),
    'ipek.ozdemir@hotmail.com', '01MNO654',
    260.00, 'USD', 'FAILED',
    'card', 'pi_seed_failed_001',
    'Card declined - insufficient funds'
);
