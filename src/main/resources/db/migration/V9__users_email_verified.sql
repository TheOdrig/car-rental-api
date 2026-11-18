ALTER TABLE car_rental.users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE car_rental.users SET email_verified = TRUE WHERE auth_provider IN ('GOOGLE', 'GITHUB');
