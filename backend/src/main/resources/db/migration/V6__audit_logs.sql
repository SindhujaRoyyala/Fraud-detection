-- =============================================================================
-- V6__audit_logs.sql - Tamper-Evident Audit Logs with Hash Chaining
-- =============================================================================

-- -----------------------------------------------------------------------------
-- AUDIT_LOGS TABLE (Hash Chained, Append-Only Ideal)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event           VARCHAR(100) NOT NULL,
    user_id         UUID REFERENCES users(id) ON DELETE SET NULL,
    username        VARCHAR(100),
    case_id         UUID REFERENCES case_files(id) ON DELETE SET NULL,
    document_id     UUID REFERENCES documents(id) ON DELETE SET NULL,
    evidence_id     UUID REFERENCES evidence(id) ON DELETE SET NULL,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(1000),
    description     VARCHAR(2000),
    metadata        JSONB,
    previous_hash   VARCHAR(64),
    entry_hash      VARCHAR(64) NOT NULL,
    block_number    BIGINT NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_audit_logs_block_number UNIQUE (block_number),
    CONSTRAINT uq_audit_logs_entry_hash UNIQUE (entry_hash)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event ON audit_logs (event);
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_case_id ON audit_logs (case_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_document_id ON audit_logs (document_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_evidence_id ON audit_logs (evidence_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_created ON audit_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_ip ON audit_logs (ip_address);
CREATE INDEX IF NOT EXISTS idx_audit_logs_block ON audit_logs (block_number ASC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_prev_hash ON audit_logs (previous_hash);

-- -----------------------------------------------------------------------------
-- UNAUTHORIZED ACCESS ATTEMPTS (separate table for security alerts)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS unauthorized_access_attempts (
    id              BIGSERIAL PRIMARY KEY,
    attempt_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(1000),
    username_tried  VARCHAR(100),
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(100),
    method          VARCHAR(20),
    path            VARCHAR(1000),
    reason          VARCHAR(500),
    resolved        BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    resolved_at     TIMESTAMP WITH TIME ZONE,
    resolution_notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_unauth_attempts_at ON unauthorized_access_attempts (attempt_at DESC);
CREATE INDEX IF NOT EXISTS idx_unauth_attempts_ip ON unauthorized_access_attempts (ip_address);
CREATE INDEX IF NOT EXISTS idx_unauth_attempts_user ON unauthorized_access_attempts (username_tried);
CREATE INDEX IF NOT EXISTS idx_unauth_attempts_resolved ON unauthorized_access_attempts (resolved) WHERE resolved = FALSE;

COMMENT ON TABLE audit_logs IS 'Hash-chained tamper-evident audit log. The entry_hash chains to previous_hash creating a blockchain.';
COMMENT ON COLUMN audit_logs.block_number IS 'Strictly incrementing sequence number for chain order';
COMMENT ON TABLE unauthorized_access_attempts IS 'Security event log for failed access attempts and security analysis';
