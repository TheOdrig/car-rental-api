CREATE INDEX idx_payments_create_time
    ON car_rental.payments (create_time)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_rentals_user_create_time
    ON car_rental.rentals (user_id, create_time DESC)
    WHERE is_deleted = FALSE;

CREATE INDEX idx_rentals_blocking_dates
    ON car_rental.rentals (start_date, end_date, car_id)
    WHERE is_deleted = FALSE AND status IN ('REQUESTED', 'CONFIRMED', 'IN_USE');
