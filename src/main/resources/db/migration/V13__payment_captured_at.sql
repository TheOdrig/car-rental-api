ALTER TABLE car_rental.payments
    ADD COLUMN captured_at TIMESTAMP(6);

UPDATE car_rental.payments
SET captured_at = update_time
WHERE status IN ('CAPTURED', 'REFUNDED');

CREATE INDEX idx_payments_captured_at
    ON car_rental.payments (captured_at)
    WHERE captured_at IS NOT NULL;
