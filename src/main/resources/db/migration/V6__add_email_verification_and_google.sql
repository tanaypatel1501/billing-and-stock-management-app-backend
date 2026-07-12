-- Email verification fields
ALTER TABLE users
    ADD COLUMN email_verified  TINYINT(1)   NOT NULL DEFAULT 0,
    ADD COLUMN verification_token VARCHAR(255) DEFAULT NULL,
    ADD COLUMN verification_token_expiry DATETIME DEFAULT NULL,
    ADD COLUMN google_id VARCHAR(255) DEFAULT NULL;

-- All existing users predate verification — mark them as already verified
-- so they aren't locked out after this deploy
UPDATE users SET email_verified = 1;

-- Index for token lookups (called on every email link click)
CREATE INDEX idx_users_verification_token ON users(verification_token);

-- Index for Google Sign-In lookups
CREATE INDEX idx_users_google_id ON users(google_id);