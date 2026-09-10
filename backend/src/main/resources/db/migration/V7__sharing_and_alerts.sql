-- =============================================================================
-- V7__sharing_and_alerts.sql - Secure Sharing & Notifications
-- =============================================================================

-- -----------------------------------------------------------------------------
-- SHARED_LINKS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shared_links (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token               VARCHAR(100) NOT NULL,
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    case_id             UUID REFERENCES case_files(id) ON DELETE SET NULL,
    created_by          UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    recipient_email     VARCHAR(255),
    recipient_name      VARCHAR(255),
    access_code_hash    VARCHAR(255),
    max_views           INTEGER,
    view_count          INTEGER NOT NULL DEFAULT 0,
    can_download        BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at          TIMESTAMP WITH TIME ZONE,
    last_accessed_at    TIMESTAMP WITH TIME ZONE,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_shared_links_token UNIQUE (token)
);

CREATE INDEX IF NOT EXISTS idx_shared_links_token ON shared_links (token);
CREATE INDEX IF NOT EXISTS idx_shared_links_document ON shared_links (document_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_created_by ON shared_links (created_by);
CREATE INDEX IF NOT EXISTS idx_shared_links_expires ON shared_links (expires_at DESC);
CREATE INDEX IF NOT EXISTS idx_shared_links_active ON shared_links (expires_at, revoked_at)
    WHERE revoked_at IS NULL;

-- -----------------------------------------------------------------------------
-- SHARED_LINK_ACCESSES (each access to a shared link)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS shared_link_accesses (
    id              BIGSERIAL PRIMARY KEY,
    shared_link_id  UUID NOT NULL REFERENCES shared_links(id) ON DELETE CASCADE,
    accessed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(1000),
    access_granted  BOOLEAN NOT NULL DEFAULT TRUE,
    denial_reason   VARCHAR(500),
    downloaded      BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_shared_accesses_link ON shared_link_accesses (shared_link_id);
CREATE INDEX IF NOT EXISTS idx_shared_accesses_at ON shared_link_accesses (accessed_at DESC);

-- -----------------------------------------------------------------------------
-- NOTIFICATIONS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            VARCHAR(100) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    message         TEXT,
    case_id         UUID REFERENCES case_files(id) ON DELETE SET NULL,
    document_id     UUID REFERENCES documents(id) ON DELETE SET NULL,
    evidence_id     UUID REFERENCES evidence(id) ON DELETE SET NULL,
    related_url     VARCHAR(1000),
    metadata        JSONB,
    is_read         BOOLEAN NOT NULL DEFAULT FALSE,
    priority        VARCHAR(50) NOT NULL DEFAULT 'NORMAL',
    read_at         TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_id ON notifications (user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user_unread ON notifications (user_id, is_read) WHERE is_read = FALSE;
CREATE INDEX IF NOT EXISTS idx_notifications_type ON notifications (type);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications (created_at DESC);

-- -----------------------------------------------------------------------------
-- SECURITY_ALERTS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS security_alerts (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_type          VARCHAR(100) NOT NULL,
    severity            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    case_id             UUID REFERENCES case_files(id) ON DELETE SET NULL,
    document_id         UUID REFERENCES documents(id) ON DELETE SET NULL,
    source_ip           VARCHAR(50),
    metadata            JSONB,
    acknowledged        BOOLEAN NOT NULL DEFAULT FALSE,
    acknowledged_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    acknowledged_at     TIMESTAMP WITH TIME ZONE,
    acknowledgement_notes TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_alerts_severity CHECK (
        severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    )
);

CREATE INDEX IF NOT EXISTS idx_security_alerts_severity ON security_alerts (severity);
CREATE INDEX IF NOT EXISTS idx_security_alerts_ack ON security_alerts (acknowledged) WHERE acknowledged = FALSE;
CREATE INDEX IF NOT EXISTS idx_security_alerts_created ON security_alerts (created_at DESC);

COMMENT ON TABLE shared_links IS 'Time-limited secure share links with optional access codes and view limits';
COMMENT ON TABLE notifications IS 'User-facing in-app notifications';
COMMENT ON TABLE security_alerts IS 'Security incident alerts for administrators';
