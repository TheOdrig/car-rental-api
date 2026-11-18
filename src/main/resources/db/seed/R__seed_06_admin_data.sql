DELETE FROM car_rental.admin_notes
WHERE user_id IN (SELECT id FROM car_rental.users WHERE email LIKE '%@example.com') AND id < 930001;

DELETE FROM car_rental.penalty_waivers
WHERE rental_id IN (SELECT id FROM car_rental.rentals WHERE user_email LIKE '%@example.com') AND id < 940001;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930001, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'ahmet.yilmaz@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'Loyal customer since January 2025. Always returns vehicles on time and in excellent condition. Consider for VIP program.',
    NOW() - INTERVAL '30 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930002, NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'ipek.ozdemir@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'Premium customer. Prefers electric and hybrid vehicles. Completed Tesla rental with excellent feedback.',
    NOW() - INTERVAL '60 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930003, NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'selin.yildiz@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'Corporate client contact. Handles bookings for ABC Corporation. Eligible for corporate discount (15%).',
    NOW() - INTERVAL '45 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930004, NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'okan.tekin@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'ATTENTION: Customer has severely late return on record (44 hours). Penalty outstanding. Require prepayment for future rentals.',
    NOW() - INTERVAL '40 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930005, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'yusuf.karaca@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'Damage dispute ongoing. Customer claims scratch was pre-existing. Review pickup photos before resolution.',
    NOW() - INTERVAL '15 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930006, NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'hasan.korkmaz@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'BANNED: Multiple late returns (5 times) over 6-month period. Two damage incidents. Customer showed pattern of negligence. Ban permanent unless reviewed by management.',
    NOW() - INTERVAL '30 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930007, NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'fatma.aslan@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'BANNED: Fraud alert. Multiple chargebacks filed. Legal team has been notified. Do not unban without legal clearance.',
    NOW() - INTERVAL '15 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.admin_notes (
    id, create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    930008, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', FALSE, 0,
    (SELECT id FROM car_rental.users WHERE email = 'kerem.tas@example.com'),
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    'admin',
    'BANNED: Terms of service violation. Customer used vehicle for commercial purposes (ride-sharing) without authorization. Contract breach documented.',
    NOW() - INTERVAL '7 days'
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    user_id = EXCLUDED.user_id,
    admin_id = EXCLUDED.admin_id,
    admin_username = EXCLUDED.admin_username,
    text = EXCLUDED.text,
    created_at = EXCLUDED.created_at;

INSERT INTO car_rental.penalty_waivers (
    id, create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    940001, NOW() - INTERVAL '150 days', NOW() - INTERVAL '150 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'merve.kilic@example.com' AND car_license_plate = '06KIA007' AND late_return_status = 'GRACE_PERIOD'),
    0.00, 0.00, 0.00,
    'Within grace period. No penalty applied. Customer was within 4-hour grace window.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '150 days',
    FALSE
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    original_penalty = EXCLUDED.original_penalty,
    waived_amount = EXCLUDED.waived_amount,
    remaining_penalty = EXCLUDED.remaining_penalty,
    reason = EXCLUDED.reason,
    admin_id = EXCLUDED.admin_id,
    waived_at = EXCLUDED.waived_at,
    refund_initiated = EXCLUDED.refund_initiated;

INSERT INTO car_rental.penalty_waivers (
    id, create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    940002, NOW() - INTERVAL '85 days', NOW() - INTERVAL '85 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'esra.dogan@example.com' AND car_license_plate = '06VLV010' AND late_return_status = 'LATE'),
    145.00, 72.50, 72.50,
    'Customer has excellent history (3 prior rentals, no issues). Late due to flight delay - documented. 50% waiver approved as goodwill gesture.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '85 days',
    FALSE
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    original_penalty = EXCLUDED.original_penalty,
    waived_amount = EXCLUDED.waived_amount,
    remaining_penalty = EXCLUDED.remaining_penalty,
    reason = EXCLUDED.reason,
    admin_id = EXCLUDED.admin_id,
    waived_at = EXCLUDED.waived_at,
    refund_initiated = EXCLUDED.refund_initiated;

INSERT INTO car_rental.penalty_waivers (
    id, create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    940003, NOW() - INTERVAL '130 days', NOW() - INTERVAL '130 days', FALSE, 0,
    (SELECT id FROM car_rental.rentals WHERE user_email = 'murat.aydin@example.com' AND car_license_plate = '35JKL789' AND late_return_status = 'LATE'),
    90.00, 22.50, 67.50,
    'Heavy rain caused traffic delays. Customer called ahead to notify. 25% waiver for communication and circumstances.',
    (SELECT id FROM car_rental.users WHERE email = 'admin@example.com'),
    NOW() - INTERVAL '130 days',
    FALSE
)
ON CONFLICT (id) DO UPDATE SET
    update_time = EXCLUDED.update_time,
    is_deleted = EXCLUDED.is_deleted,
    version = EXCLUDED.version,
    rental_id = EXCLUDED.rental_id,
    original_penalty = EXCLUDED.original_penalty,
    waived_amount = EXCLUDED.waived_amount,
    remaining_penalty = EXCLUDED.remaining_penalty,
    reason = EXCLUDED.reason,
    admin_id = EXCLUDED.admin_id,
    waived_at = EXCLUDED.waived_at,
    refund_initiated = EXCLUDED.refund_initiated;
