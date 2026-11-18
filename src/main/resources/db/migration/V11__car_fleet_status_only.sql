DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM car_rental.car WHERE damage_price IS NOT NULL AND damage_price <> 0) THEN
        RAISE EXCEPTION 'car.damage_price holds non-zero values; review and zero them before this migration';
    END IF;
END $$;

UPDATE car_rental.car
SET car_status_type = 'AVAILABLE'
WHERE car_status_type IN ('RENTED', 'RESERVED');

ALTER TABLE car_rental.car
    DROP COLUMN damage_price;

ALTER TABLE car_rental.car
    ADD CONSTRAINT chk_car_status CHECK (car_status_type IN ('AVAILABLE', 'MAINTENANCE', 'DAMAGED', 'INSPECTION'));
