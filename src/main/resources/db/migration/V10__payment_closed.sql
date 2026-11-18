ALTER TABLE car_rental.payments ADD COLUMN closed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE car_rental.payments p
SET closed = TRUE
FROM car_rental.rentals r
WHERE p.rental_id = r.id AND r.status = 'CANCELLED';
