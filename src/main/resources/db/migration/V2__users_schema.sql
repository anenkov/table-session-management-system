CREATE TABLE IF NOT EXISTS app_user (
  id            UUID PRIMARY KEY,
  username      VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(30)  NOT NULL,
  is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_app_user_role ON app_user(role);
CREATE INDEX IF NOT EXISTS idx_app_user_active ON app_user(is_active);
