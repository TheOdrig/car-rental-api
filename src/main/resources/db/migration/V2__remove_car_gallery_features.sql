UPDATE car_rental.car
SET is_deleted = TRUE,
    car_status_type = 'MAINTENANCE'
WHERE car_status_type = 'SOLD';

ALTER TABLE car_rental.car
    DROP COLUMN is_featured,
    DROP COLUMN is_test_drive_available,
    DROP COLUMN view_count,
    DROP COLUMN like_count;
