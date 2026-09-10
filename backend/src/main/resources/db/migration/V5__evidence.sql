-- =============================================================================
-- V5__evidence.sql - Evidence Registry and Chain of Custody
-- =============================================================================

-- -----------------------------------------------------------------------------
-- EVIDENCE TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidence (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id             UUID NOT NULL REFERENCES case_files(id) ON DELETE CASCADE,
    document_id         UUID REFERENCES documents(id) ON DELETE SET NULL,
    evidence_number     VARCHAR(50) NOT NULL,
    title               VARCHAR(500) NOT NULL,
    description         TEXT,
    evidence_type       VARCHAR(100),
    status              VARCHAR(50) NOT NULL DEFAULT 'COLLECTED',
    collection_location VARCHAR(1000),
    collected_at        TIMESTAMP WITH TIME ZONE,
    collected_by        UUID REFERENCES users(id) ON DELETE SET NULL,
    current_custodian_id UUID REFERENCES users(id) ON DELETE SET NULL,
    chain_hash          VARCHAR(64),
    qr_code_data        VARCHAR(2000),
    metadata            JSONB,
    classification      VARCHAR(100),
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_evidence_number UNIQUE (evidence_number),
    CONSTRAINT chk_evidence_status CHECK (
        status IN ('COLLECTED', 'IN_CUSTODY', 'TRANSFERRED', 'ANALYZED',
                   'PRESENTED', 'RETURNED', 'DESTROYED', 'ARCHIVED')
    )
);

CREATE INDEX IF NOT EXISTS idx_evidence_case_id ON evidence (case_id);
CREATE INDEX IF NOT EXISTS idx_evidence_number ON evidence (evidence_number);
CREATE INDEX IF NOT EXISTS idx_evidence_status ON evidence (status);
CREATE INDEX IF NOT EXISTS idx_evidence_document ON evidence (document_id);
CREATE INDEX IF NOT EXISTS idx_evidence_custodian ON evidence (current_custodian_id);
CREATE INDEX IF NOT EXISTS idx_evidence_collected_by ON evidence (collected_by);
CREATE INDEX IF NOT EXISTS idx_evidence_created ON evidence (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_evidence_chain_hash ON evidence (chain_hash);

DROP TRIGGER IF EXISTS trg_evidence_update_timestamp ON evidence;
CREATE TRIGGER trg_evidence_update_timestamp
    BEFORE UPDATE ON evidence
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- -----------------------------------------------------------------------------
-- EVIDENCE_TRANSFERS - Chain of Custody Entries (Hash Chained)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidence_transfers (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evidence_id         UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    from_user_id        UUID REFERENCES users(id) ON DELETE SET NULL,
    to_user_id          UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    transfer_reason     TEXT,
    location            VARCHAR(1000),
    witness_name        VARCHAR(255),
    notes               TEXT,
    previous_chain_hash VARCHAR(64),
    transfer_hash       VARCHAR(64) NOT NULL,
    transfer_number     VARCHAR(50),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    received_at         TIMESTAMP WITH TIME ZONE,
    receiver_ip         VARCHAR(50)
);

CREATE INDEX IF NOT EXISTS idx_evidence_transfers_evidence ON evidence_transfers (evidence_id);
CREATE INDEX IF NOT EXISTS idx_evidence_transfers_from ON evidence_transfers (from_user_id);
CREATE INDEX IF NOT EXISTS idx_evidence_transfers_to ON evidence_transfers (to_user_id);
CREATE INDEX IF NOT EXISTS idx_evidence_transfers_created ON evidence_transfers (created_at ASC);
CREATE INDEX IF NOT EXISTS idx_evidence_transfers_hash ON evidence_transfers (transfer_hash);

-- -----------------------------------------------------------------------------
-- EVIDENCE IMAGES / ATTACHMENTS (e.g., photos of physical evidence)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS evidence_attachments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    evidence_id     UUID NOT NULL REFERENCES evidence(id) ON DELETE CASCADE,
    file_name       VARCHAR(500) NOT NULL,
    storage_key     VARCHAR(1000) NOT NULL,
    mime_type       VARCHAR(200),
    file_size_bytes BIGINT NOT NULL,
    sha256_hash     VARCHAR(64) NOT NULL,
    caption         VARCHAR(1000),
    uploaded_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidence_attachments_ev ON evidence_attachments (evidence_id);
CREATE INDEX IF NOT EXISTS idx_evidence_attachments_hash ON evidence_attachments (sha256_hash);

COMMENT ON TABLE evidence IS 'Evidence registry for legal and investigative artifacts';
COMMENT ON TABLE evidence_transfers IS 'Hash-chained chain of custody transfer log for each evidence item';
COMMENT ON COLUMN evidence_transfers.transfer_hash IS 'SHA-256 hash chaining this transfer to the previous one';
