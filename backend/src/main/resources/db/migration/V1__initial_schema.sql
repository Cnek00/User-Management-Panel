-- Initial schema for the User Management Panel.
-- Hibernate runs in validate mode; Flyway is the single source of truth.

CREATE TABLE ranks (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(255) NOT NULL UNIQUE,
    level INTEGER      NOT NULL
);

CREATE TABLE roles (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    action          VARCHAR(255) NOT NULL UNIQUE,
    is_delegatable  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE scopes (
    id          BIGSERIAL PRIMARY KEY,
    scope_key   VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE app_users (
    id       BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    email    VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    enabled  BOOLEAN      NOT NULL DEFAULT TRUE,
    rank_id  BIGINT REFERENCES ranks (id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE user_permissions (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES app_users (id) ON DELETE CASCADE,
    permission_id  BIGINT NOT NULL REFERENCES permissions (id),
    scope_id       BIGINT NOT NULL REFERENCES scopes (id),
    granted_by_id  BIGINT REFERENCES app_users (id) ON DELETE SET NULL,
    CONSTRAINT uq_user_permission_scope UNIQUE (user_id, permission_id, scope_id)
);

CREATE INDEX idx_app_users_rank ON app_users (rank_id);
CREATE INDEX idx_user_permissions_user ON user_permissions (user_id);
