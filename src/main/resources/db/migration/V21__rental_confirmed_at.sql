ALTER TABLE car_rental.rentals ADD COLUMN confirmed_at TIMESTAMPTZ;

UPDATE car_rental.rentals SET confirmed_at = update_time WHERE status = 'CONFIRMED';
