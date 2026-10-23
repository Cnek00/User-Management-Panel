-- Demo data for local development and the portfolio screenshots.
-- Passwords are BCrypt hashes of the credentials listed in the README.
-- Change them before exposing the application publicly.

INSERT INTO ranks (name, level) VALUES
    ('SUPER_ADMIN', 100),
    ('ADMIN', 80),
    ('MODERATOR', 50),
    ('EDITOR', 30),
    ('USER', 10),
    ('GUEST', 1);

INSERT INTO roles (name) VALUES
    ('ROLE_SUPER_ADMIN'),
    ('ROLE_ADMIN'),
    ('ROLE_MODERATOR'),
    ('ROLE_USER');

INSERT INTO permissions (action, is_delegatable) VALUES
    ('READ', FALSE),
    ('WRITE', TRUE),
    ('DELETE', TRUE),
    ('MANAGE_USERS', TRUE),
    ('VIEW_REPORTS', FALSE),
    ('EXPORT_DATA', TRUE),
    ('MANAGE_SETTINGS', FALSE);

INSERT INTO scopes (scope_key, description) VALUES
    ('SYSTEM', 'Full system access'),
    ('FINANCE_MODULE', 'Finance and accounting module'),
    ('HR_MODULE', 'Human resources module'),
    ('PROJECT_MODULE', 'Project management module'),
    ('INVENTORY_MODULE', 'Inventory and stock module'),
    ('REPORT_MODULE', 'Reporting module');

INSERT INTO app_users (username, email, password, enabled, rank_id)
SELECT 'admin', 'admin@example.com', '$2y$10$Q4rRoXbtQz2z4AzhKLqBCOidR.l7KcfYyCep4LeSoo5QqksFIkwfS', TRUE, id
FROM ranks WHERE name = 'SUPER_ADMIN';

INSERT INTO app_users (username, email, password, enabled, rank_id)
SELECT 'moderator', 'moderator@example.com', '$2y$10$IL54.yybAvbmBvUtF8/xFu45SOA8upXpmqVSksFx8xeeOHyOrULVG', TRUE, id
FROM ranks WHERE name = 'MODERATOR';

INSERT INTO app_users (username, email, password, enabled, rank_id)
SELECT 'user', 'user@example.com', '$2y$10$i7I/3SPx6Gv4kcO0Nu0pAuYXxNNQlXNGzEIlfKIxrEOdnDVG8qygS', TRUE, id
FROM ranks WHERE name = 'USER';

INSERT INTO app_users (username, email, password, enabled, rank_id)
SELECT 'guest', 'guest@example.com', '$2y$10$nuBIwKEEa7DsEDmDbX23.usY2n6owHM.3lLd94ThGILqUQZ6pEpL6', FALSE, id
FROM ranks WHERE name = 'GUEST';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
JOIN roles r ON r.name IN ('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')
WHERE u.username = 'admin';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
JOIN roles r ON r.name = 'ROLE_MODERATOR'
WHERE u.username = 'moderator';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM app_users u
JOIN roles r ON r.name = 'ROLE_USER'
WHERE u.username IN ('user', 'guest');

-- One example of a direct, scoped permission grant.
INSERT INTO user_permissions (user_id, permission_id, scope_id, granted_by_id)
SELECT target.id, p.id, s.id, admin.id
FROM app_users target
JOIN app_users admin ON admin.username = 'admin'
JOIN permissions p ON p.action = 'VIEW_REPORTS'
JOIN scopes s ON s.scope_key = 'REPORT_MODULE'
WHERE target.username = 'moderator';
