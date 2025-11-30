ALTER TABLE gallery.users
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE gallery.users 
    ADD COLUMN avatar_url VARCHAR(500);

ALTER TABLE gallery.users 
    ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL';

CREATE TABLE IF NOT EXISTS gallery.linked_accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_linked_account_user FOREIGN KEY (user_id) REFERENCES gallery.users(id) ON DELETE CASCADE,
    CONSTRAINT uq_linked_account_provider_id UNIQUE (provider, provider_id)
);

CREATE INDEX IF NOT EXISTS idx_linked_accounts_user_id ON gallery.linked_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_linked_accounts_provider_email ON gallery.linked_accounts(provider, email);
CREATE INDEX IF NOT EXISTS idx_linked_accounts_provider_provider_id ON gallery.linked_accounts(provider, provider_id);

UPDATE gallery.users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
