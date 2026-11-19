ALTER TABLE car_rental.users ADD COLUMN last_login_at TIMESTAMPTZ;

ALTER TABLE car_rental.rentals ADD COLUMN user_username VARCHAR(255);

UPDATE car_rental.rentals r
SET user_username = u.username
FROM car_rental.users u
WHERE u.id = r.user_id;
