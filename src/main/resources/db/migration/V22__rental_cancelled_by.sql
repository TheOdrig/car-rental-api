ALTER TABLE car_rental.rentals ADD COLUMN cancelled_by VARCHAR(20);

UPDATE car_rental.rentals
SET cancelled_by = 'SYSTEM'
WHERE status = 'CANCELLED'
  AND cancellation_reason IN (
      'Request expired without approval',
      'Payment not completed before the start date',
      'Payment not completed before the payment deadline',
      'Vehicle was not picked up after the start date');

ALTER TABLE car_rental.rentals
    ADD CONSTRAINT chk_rental_cancelled_by CHECK (cancelled_by IS NULL OR cancelled_by IN ('CUSTOMER', 'ADMIN', 'SYSTEM'));
