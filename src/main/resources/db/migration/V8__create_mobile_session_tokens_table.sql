-- Long-lived session token issued only on mobile app logins (Capacitor
-- Android/iOS), separate from the short-lived (30 min) JWT access token
-- used everywhere else. Lets the mobile app silently obtain a new access
-- token after the original one expires, without forcing re-login, while
-- staying fully revocable (e.g. on explicit logout) unlike a bare JWT.
CREATE TABLE mobile_session_tokens (
                                       id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       token       VARCHAR(512) NOT NULL,
                                       user_id     BIGINT NOT NULL,
                                       expiry_date DATETIME(6) NOT NULL,
                                       revoked     TINYINT(1) NOT NULL DEFAULT 0,
                                       created_at  DATETIME(6) NOT NULL,
                                       CONSTRAINT uq_mobile_session_token UNIQUE (token),
                                       CONSTRAINT fk_mobile_session_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Looked up on every silent-renew request from the app — needs to be fast
CREATE INDEX idx_mobile_session_token ON mobile_session_tokens(token);

-- Supports "revoke all sessions for this user" (e.g. on password change)
-- and general per-user lookups
CREATE INDEX idx_mobile_session_user ON mobile_session_tokens(user_id);