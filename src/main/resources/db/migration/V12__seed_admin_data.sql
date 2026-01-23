-- =============================================================================
-- V12: Seed Admin Data (Admin Notes + Penalty Waivers)
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Admin Notes (8 notes on various customers)
-- -----------------------------------------------------------------------------

-- Notes on active customers with good standing
INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ahmet.yilmaz@gmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'Loyal customer since January 2025. Always returns vehicles on time and in excellent condition. Consider for VIP program.',
    NOW() - INTERVAL '30 days'
);

INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '60 days', NOW() - INTERVAL '60 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'ipek.ozdemir@hotmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'Premium customer. Prefers electric and hybrid vehicles. Completed Tesla rental with excellent feedback.',
    NOW() - INTERVAL '60 days'
);

INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '45 days', NOW() - INTERVAL '45 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'selin.yildiz@gmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'Corporate client contact. Handles bookings for ABC Corporation. Eligible for corporate discount (15%).',
    NOW() - INTERVAL '45 days'
);

-- Notes on customers with issues
INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '40 days', NOW() - INTERVAL '40 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'okan.tekin@outlook.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'ATTENTION: Customer has severely late return on record (44 hours). Penalty outstanding. Require prepayment for future rentals.',
    NOW() - INTERVAL '40 days'
);

INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'yusuf.karaca@gmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'Damage dispute ongoing. Customer claims scratch was pre-existing. Review pickup photos before resolution.',
    NOW() - INTERVAL '15 days'
);

-- Notes on banned customers (documentation)
INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '30 days', NOW() - INTERVAL '30 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'hasan.korkmaz@gmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'BANNED: Multiple late returns (5 times) over 6-month period. Two damage incidents. Customer showed pattern of negligence. Ban permanent unless reviewed by management.',
    NOW() - INTERVAL '30 days'
);

INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '15 days', NOW() - INTERVAL '15 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'fatma.aslan@hotmail.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'BANNED: Fraud alert. Multiple chargebacks filed. Legal team has been notified. Do not unban without legal clearance.',
    NOW() - INTERVAL '15 days'
);

INSERT INTO gallery.admin_notes (
    create_time, update_time, is_deleted, version,
    user_id, admin_id, admin_username, text, created_at
) VALUES (
    NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days', FALSE, 0,
    (SELECT id FROM gallery.users WHERE email = 'kerem.tas@yahoo.com'),
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    'admin',
    'BANNED: Terms of service violation. Customer used vehicle for commercial purposes (ride-sharing) without authorization. Contract breach documented.',
    NOW() - INTERVAL '7 days'
);

-- -----------------------------------------------------------------------------
-- Penalty Waivers (3 waivers for late returns)
-- -----------------------------------------------------------------------------

-- Full waiver (100%) - First-time offender
INSERT INTO gallery.penalty_waivers (
    create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    NOW() - INTERVAL '150 days', NOW() - INTERVAL '150 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'merve.kilic@yahoo.com' AND car_license_plate = '06KIA007' AND late_return_status = 'GRACE_PERIOD'),
    0.00, 0.00, 0.00,
    'Within grace period. No penalty applied. Customer was within 4-hour grace window.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '150 days',
    FALSE
);

-- Partial waiver (50%) - Good customer history
INSERT INTO gallery.penalty_waivers (
    create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    NOW() - INTERVAL '85 days', NOW() - INTERVAL '85 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'esra.dogan@hotmail.com' AND car_license_plate = '06VLV010' AND late_return_status = 'LATE'),
    145.00, 72.50, 72.50,
    'Customer has excellent history (3 prior rentals, no issues). Late due to flight delay - documented. 50% waiver approved as goodwill gesture.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '85 days',
    FALSE
);

-- Partial waiver (25%) - Weather circumstances
INSERT INTO gallery.penalty_waivers (
    create_time, update_time, is_deleted, version,
    rental_id, original_penalty, waived_amount, remaining_penalty,
    reason, admin_id, waived_at, refund_initiated
) VALUES (
    NOW() - INTERVAL '130 days', NOW() - INTERVAL '130 days', FALSE, 0,
    (SELECT id FROM gallery.rentals WHERE user_email = 'murat.aydin@outlook.com' AND car_license_plate = '35JKL789' AND late_return_status = 'LATE'),
    90.00, 22.50, 67.50,
    'Heavy rain caused traffic delays. Customer called ahead to notify. 25% waiver for communication and circumstances.',
    (SELECT id FROM gallery.users WHERE email = 'admin@carrental.com'),
    NOW() - INTERVAL '130 days',
    FALSE
);
