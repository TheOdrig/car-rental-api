DELETE FROM car_rental.payments
WHERE user_email LIKE '%@example.com' AND id < 910001;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910001, '2025-01-10 10:00:00'::timestamp, '2025-01-10 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ahmet.yilmaz@example.com' AND car_license_plate = '34ABC123' AND start_date = '2025-01-10'),
    'ahmet.yilmaz@example.com', '34ABC123',
    175.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jan_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910002, '2025-01-18 09:30:00'::timestamp, '2025-01-18 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'zeynep.kaya@example.com' AND car_license_plate = '16RNL005' AND start_date = '2025-01-18'),
    'zeynep.kaya@example.com', '16RNL005',
    280.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jan_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910003, '2025-02-05 11:00:00'::timestamp, '2025-02-05 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'mehmet.demir@example.com' AND car_license_plate = '16XYZ321' AND start_date = '2025-02-05'),
    'mehmet.demir@example.com', '16XYZ321',
    225.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_feb_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910004, '2025-02-15 14:00:00'::timestamp, '2025-02-15 14:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ayse.ozturk@example.com' AND car_license_plate = '34HYN006' AND start_date = '2025-02-15'),
    'ayse.ozturk@example.com', '34HYN006',
    680.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_feb_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910005, '2025-03-01 10:30:00'::timestamp, '2025-03-01 10:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'can.arslan@example.com' AND car_license_plate = '06DEF456' AND start_date = '2025-03-01'),
    'can.arslan@example.com', '06DEF456',
    475.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910006, '2025-03-15 15:00:00'::timestamp, '2025-03-15 15:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'elif.sahin@example.com' AND car_license_plate = '16CTR015' AND start_date = '2025-03-15'),
    'elif.sahin@example.com', '16CTR015',
    114.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910007, '2025-03-22 09:00:00'::timestamp, '2025-03-22 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'burak.celik@example.com' AND car_license_plate = '01MNO654' AND start_date = '2025-03-22'),
    'burak.celik@example.com', '01MNO654',
    260.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_mar_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910008, '2025-04-05 10:00:00'::timestamp, '2025-04-05 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'selin.yildiz@example.com' AND car_license_plate = '34MRC001' AND start_date = '2025-04-05'),
    'selin.yildiz@example.com', '34MRC001',
    840.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910009, '2025-04-18 11:30:00'::timestamp, '2025-04-18 11:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'emre.koc@example.com' AND car_license_plate = '35FIA004' AND start_date = '2025-04-18'),
    'emre.koc@example.com', '35FIA004',
    126.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910010, '2025-04-25 14:00:00'::timestamp, '2025-04-25 14:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'deniz.aksoy@example.com' AND car_license_plate = '35MZD008' AND start_date = '2025-04-25'),
    'deniz.aksoy@example.com', '35MZD008',
    110.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_apr_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910011, '2025-05-05 09:00:00'::timestamp, '2025-05-05 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'gokhan.erdogan@example.com' AND car_license_plate = '06VLV010' AND start_date = '2025-05-05'),
    'gokhan.erdogan@example.com', '06VLV010',
    725.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910012, '2025-05-15 10:30:00'::timestamp, '2025-05-15 10:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'merve.kilic@example.com' AND car_license_plate = '06KIA007' AND start_date = '2025-05-15'),
    'merve.kilic@example.com', '06KIA007',
    400.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910013, '2025-05-25 11:00:00'::timestamp, '2025-05-25 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ali.polat@example.com' AND car_license_plate = '16SKD011' AND start_date = '2025-05-25'),
    'ali.polat@example.com', '16SKD011',
    180.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_may_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910014, '2025-06-01 08:30:00'::timestamp, '2025-06-01 08:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ipek.ozdemir@example.com' AND car_license_plate = '34TSL002' AND start_date = '2025-06-01'),
    'ipek.ozdemir@example.com', '34TSL002',
    1800.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910015, '2025-06-15 09:00:00'::timestamp, '2025-06-15 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'murat.aydin@example.com' AND car_license_plate = '35JKL789' AND start_date = '2025-06-15'),
    'murat.aydin@example.com', '35JKL789',
    720.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910016, '2025-06-25 10:00:00'::timestamp, '2025-06-25 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ceren.ozer@example.com' AND car_license_plate = '06FRD003' AND start_date = '2025-06-25'),
    'ceren.ozer@example.com', '06FRD003',
    240.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jun_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910017, '2025-07-01 09:30:00'::timestamp, '2025-07-01 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'yusuf.karaca@example.com' AND car_license_plate = '34MRC001' AND start_date = '2025-07-01'),
    'yusuf.karaca@example.com', '34MRC001',
    1200.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910018, '2025-07-15 10:00:00'::timestamp, '2025-07-15 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ahmet.yilmaz@example.com' AND car_license_plate = '34HYN006' AND start_date = '2025-07-15'),
    'ahmet.yilmaz@example.com', '34HYN006',
    595.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910019, '2025-07-25 11:30:00'::timestamp, '2025-07-25 11:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'zeynep.kaya@example.com' AND car_license_plate = '06OPL014' AND start_date = '2025-07-25'),
    'zeynep.kaya@example.com', '06OPL014',
    126.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_jul_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910020, '2025-08-05 08:00:00'::timestamp, '2025-08-05 08:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'esra.dogan@example.com' AND car_license_plate = '06VLV010' AND start_date = '2025-08-05'),
    'esra.dogan@example.com', '06VLV010',
    1160.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_aug_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910021, '2025-08-20 09:30:00'::timestamp, '2025-08-20 09:30:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'serkan.yilmaz@example.com' AND car_license_plate = '34NSN012' AND start_date = '2025-08-20'),
    'serkan.yilmaz@example.com', '34NSN012',
    375.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_aug_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910022, '2025-09-10 10:00:00'::timestamp, '2025-09-10 10:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'pinar.sen@example.com' AND car_license_plate = '06DEF456' AND start_date = '2025-09-10'),
    'pinar.sen@example.com', '06DEF456',
    475.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_sep_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910023, '2025-09-20 11:00:00'::timestamp, '2025-09-20 11:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'okan.tekin@example.com' AND car_license_plate = '35SEA013' AND start_date = '2025-09-20'),
    'okan.tekin@example.com', '35SEA013',
    405.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_sep_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910024, '2025-10-15 09:00:00'::timestamp, '2025-10-15 09:00:00'::timestamp, FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'mehmet.demir@example.com' AND car_license_plate = '16XYZ321' AND start_date = '2025-10-15'),
    'mehmet.demir@example.com', '16XYZ321',
    225.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_oct_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910025, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'can.arslan@example.com' AND car_license_plate = '35FIA004' AND status = 'IN_USE'),
    'can.arslan@example.com', '35FIA004',
    210.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910026, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'elif.sahin@example.com' AND car_license_plate = '34NSN012' AND status = 'IN_USE'),
    'elif.sahin@example.com', '34NSN012',
    450.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910027, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'burak.celik@example.com' AND car_license_plate = '34TSL002' AND status = 'IN_USE'),
    'burak.celik@example.com', '34TSL002',
    900.00, 'USD', 'CAPTURED',
    'card', 'pi_seed_current_003'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910028, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'selin.yildiz@example.com' AND car_license_plate = '06KIA007' AND status = 'CONFIRMED'),
    'selin.yildiz@example.com', '06KIA007',
    560.00, 'USD', 'PENDING',
    'card', 'pi_seed_pending_001'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id
) VALUES (
    910029, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'emre.koc@example.com' AND car_license_plate = '34MRC001' AND status = 'CONFIRMED'),
    'emre.koc@example.com', '34MRC001',
    600.00, 'USD', 'PENDING',
    'card', 'pi_seed_pending_002'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id,
    refunded_amount
) VALUES (
    910030, NOW() - INTERVAL '20 days', NOW() - INTERVAL '18 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ali.polat@example.com' AND car_license_plate = '16RNL005' AND status = 'CANCELLED'),
    'ali.polat@example.com', '16RNL005',
    200.00, 'USD', 'REFUNDED',
    'card', 'pi_seed_refund_001',
    200.00
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id,
    refunded_amount = EXCLUDED.refunded_amount;

INSERT INTO car_rental.payments (
    id, create_time, update_time, is_deleted, version,
    rental_id, user_email, car_license_plate,
    amount, currency, status,
    payment_method, stripe_payment_intent_id,
    failure_reason
) VALUES (
    910031, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'ipek.ozdemir@example.com' AND car_license_plate = '01MNO654' AND status = 'CANCELLED'),
    'ipek.ozdemir@example.com', '01MNO654',
    260.00, 'USD', 'FAILED',
    'card', 'pi_seed_failed_001',
    'Card declined - insufficient funds'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    user_email = EXCLUDED.user_email,
    car_license_plate = EXCLUDED.car_license_plate,
    amount = EXCLUDED.amount,
    currency = EXCLUDED.currency,
    status = EXCLUDED.status,
    payment_method = EXCLUDED.payment_method,
    stripe_payment_intent_id = EXCLUDED.stripe_payment_intent_id,
    failure_reason = EXCLUDED.failure_reason;
