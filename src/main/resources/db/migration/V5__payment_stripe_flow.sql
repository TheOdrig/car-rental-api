ALTER TABLE car_rental.payments
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'RENTAL',
    ADD COLUMN intent_attempt INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN refund_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN reference_id BIGINT,
    ADD COLUMN stripe_customer_id VARCHAR(255),
    ADD COLUMN stripe_payment_method_id VARCHAR(255);

ALTER TABLE car_rental.payments
    ADD CONSTRAINT chk_payment_kind CHECK (kind IN ('RENTAL', 'PENALTY', 'DAMAGE'));

CREATE UNIQUE INDEX uk_payments_rental_kind
    ON car_rental.payments (rental_id)
    WHERE kind = 'RENTAL' AND is_deleted = FALSE;

CREATE UNIQUE INDEX uk_payments_stripe_intent
    ON car_rental.payments (stripe_payment_intent_id)
    WHERE stripe_payment_intent_id IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX uk_payments_damage_reference
    ON car_rental.payments (reference_id)
    WHERE kind = 'DAMAGE' AND reference_id IS NOT NULL AND is_deleted = FALSE;
