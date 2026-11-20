ALTER TABLE car_rental.rentals ADD COLUMN picked_up_at TIMESTAMPTZ;
ALTER TABLE car_rental.rentals ADD COLUMN original_price NUMERIC(12, 2);
ALTER TABLE car_rental.rentals ADD COLUMN price_adjustments TEXT;

UPDATE car_rental.rentals
SET picked_up_at = LEAST(
        COALESCE(actual_return_time, update_time),
        GREATEST(COALESCE(confirmed_at, create_time), start_date::timestamp AT TIME ZONE 'UTC'))
WHERE status IN ('IN_USE', 'RETURNED');
