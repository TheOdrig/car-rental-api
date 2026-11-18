CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE car_rental.rentals
    ADD CONSTRAINT ex_rentals_car_no_overlap
    EXCLUDE USING gist (
        car_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    )
    WHERE (is_deleted = FALSE AND status IN ('REQUESTED', 'CONFIRMED', 'IN_USE'));
