ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('TRADER', 'RISK', 'ADMIN'));