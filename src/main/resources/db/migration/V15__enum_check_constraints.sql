UPDATE car_rental.damage_reports
SET category = CASE category
        WHEN 'INTERIOR' THEN 'INTERIOR_DAMAGE'
        WHEN 'TIRE' THEN 'TIRE_DAMAGE'
        WHEN 'GLASS' THEN 'GLASS_DAMAGE'
        WHEN 'MECHANICAL' THEN 'MECHANICAL_DAMAGE'
        ELSE category
    END,
    severity = CASE severity
        WHEN 'SEVERE' THEN 'MAJOR'
        ELSE severity
    END;

ALTER TABLE car_rental.damage_reports
    ADD CONSTRAINT chk_damage_severity CHECK (severity IS NULL OR severity IN ('MINOR', 'MODERATE', 'MAJOR', 'TOTAL_LOSS')),
    ADD CONSTRAINT chk_damage_category CHECK (category IS NULL OR category IN
        ('SCRATCH', 'DENT', 'GLASS_DAMAGE', 'TIRE_DAMAGE', 'INTERIOR_DAMAGE', 'MECHANICAL_DAMAGE')),
    ADD CONSTRAINT chk_damage_status CHECK (status IN
        ('REPORTED', 'UNDER_ASSESSMENT', 'ASSESSED', 'CHARGED', 'DISPUTED', 'RESOLVED'));

ALTER TABLE car_rental.rentals
    ADD CONSTRAINT chk_rental_status CHECK (status IN ('REQUESTED', 'CONFIRMED', 'IN_USE', 'RETURNED', 'CANCELLED')),
    ADD CONSTRAINT chk_rental_late_return_status CHECK (late_return_status IS NULL OR late_return_status IN
        ('ON_TIME', 'GRACE_PERIOD', 'LATE', 'SEVERELY_LATE'));

ALTER TABLE car_rental.payments
    ADD CONSTRAINT chk_payment_status CHECK (status IN
        ('PENDING', 'AUTHORIZED', 'CAPTURED', 'FAILED', 'REFUNDED', 'CANCELLED'));
