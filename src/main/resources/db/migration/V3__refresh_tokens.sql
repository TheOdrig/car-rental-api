CREATE TABLE car_rental.refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    issued_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    replaced_by_id BIGINT,
    CONSTRAINT uk_refresh_tokens_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES car_rental.users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced FOREIGN KEY (replaced_by_id) REFERENCES car_rental.refresh_tokens (id) ON DELETE SET NULL
);

CREATE INDEX idx_refresh_tokens_user ON car_rental.refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_family ON car_rental.refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_expires ON car_rental.refresh_tokens (expires_at);
