CREATE TABLE car_rental.installation_identity (
    id SMALLINT PRIMARY KEY DEFAULT 1,
    installation_id UUID NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT (now() AT TIME ZONE 'UTC'),
    CONSTRAINT ck_installation_identity_single_row CHECK (id = 1)
);

INSERT INTO car_rental.installation_identity (id, installation_id) VALUES (1, gen_random_uuid());
