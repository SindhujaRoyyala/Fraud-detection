-- =============================================================================
-- V3__case_management.sql - Case Management Schema
-- =============================================================================

-- -----------------------------------------------------------------------------
-- CASE_FILES TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS case_files (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_number                 VARCHAR(50) NOT NULL,
    title                       VARCHAR(500) NOT NULL,
    description                 TEXT,
    status                      VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    priority                    VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    case_type                   VARCHAR(100),
    jurisdiction                VARCHAR(200),
    metadata                    JSONB,
    assigned_investigator_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    assigned_legal_officer_id   UUID REFERENCES users(id) ON DELETE SET NULL,
    opened_at                   TIMESTAMP WITH TIME ZONE,
    closed_at                   TIMESTAMP WITH TIME ZONE,
    created_by                  UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_case_files_case_number UNIQUE (case_number),
    CONSTRAINT chk_case_files_status CHECK (
        status IN ('DRAFT', 'ACTIVE', 'REVIEWED', 'CLOSED', 'ARCHIVED')
    ),
    CONSTRAINT chk_case_files_priority CHECK (
        priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')
    )
);

CREATE INDEX IF NOT EXISTS idx_case_files_status ON case_files (status);
CREATE INDEX IF NOT EXISTS idx_case_files_priority ON case_files (priority);
CREATE INDEX IF NOT EXISTS idx_case_files_created_by ON case_files (created_by);
CREATE INDEX IF NOT EXISTS idx_case_files_created_at ON case_files (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_case_files_investigator ON case_files (assigned_investigator_id);
CREATE INDEX IF NOT EXISTS idx_case_files_legal_officer ON case_files (assigned_legal_officer_id);
CREATE INDEX IF NOT EXISTS idx_case_files_case_number ON case_files (case_number);
CREATE INDEX IF NOT EXISTS idx_case_files_search ON case_files USING GIN (
    to_tsvector('english', COALESCE(title, '') || ' ' || COALESCE(description, ''))
);

DROP TRIGGER IF EXISTS trg_case_files_update_timestamp ON case_files;
CREATE TRIGGER trg_case_files_update_timestamp
    BEFORE UPDATE ON case_files
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- -----------------------------------------------------------------------------
-- CASE_ASSIGNMENTS HISTORY (audit trail of assignments over time)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS case_assignments (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id             UUID NOT NULL REFERENCES case_files(id) ON DELETE CASCADE,
    assigned_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    old_investigator    UUID REFERENCES users(id) ON DELETE SET NULL,
    new_investigator    UUID REFERENCES users(id) ON DELETE SET NULL,
    old_legal_officer   UUID REFERENCES users(id) ON DELETE SET NULL,
    new_legal_officer   UUID REFERENCES users(id) ON DELETE SET NULL,
    notes               TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_assignments_case_id ON case_assignments (case_id);
CREATE INDEX IF NOT EXISTS idx_case_assignments_created ON case_assignments (created_at DESC);

COMMENT ON TABLE case_files IS 'Primary case file records for investigations';
COMMENT ON TABLE case_assignments IS 'Audit trail of case assignment changes';
