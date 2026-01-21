-- =============================================================================
-- V2: Users Table with OAuth2 Support
-- =============================================================================

-- -----------------------------------------------------------------------------
-- Users Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255),
    first_name VARCHAR(50),
    last_name VARCHAR(50),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    auth_provider VARCHAR(20) DEFAULT 'LOCAL',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT DEFAULT 0
);

-- -----------------------------------------------------------------------------
-- User Roles Table
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS gallery.user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    FOREIGN KEY (user_id) REFERENCES gallery.users(id) ON DELETE CASCADE
);

-- -----------------------------------------------------------------------------
-- Linked Accounts (OAuth2)
-- -----------------------------------------------------------------------------
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

-- Indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON gallery.users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON gallery.users(email);
CREATE INDEX IF NOT EXISTS idx_users_enabled ON gallery.users(enabled);
CREATE INDEX IF NOT EXISTS idx_linked_accounts_user_id ON gallery.linked_accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_linked_accounts_provider_email ON gallery.linked_accounts(provider, email);
CREATE INDEX IF NOT EXISTS idx_linked_accounts_provider_provider_id ON gallery.linked_accounts(provider, provider_id);
