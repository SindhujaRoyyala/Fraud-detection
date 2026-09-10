-- =============================================================================
-- V8__ai_and_search.sql - AI Processing, Timeline & Search Support
-- =============================================================================

-- -----------------------------------------------------------------------------
-- AI_PROCESSING_JOBS TABLE - Tracks async AI processing tasks
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ai_processing_jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    job_type            VARCHAR(100) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'QUEUED',
    queued_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at          TIMESTAMP WITH TIME ZONE,
    completed_at        TIMESTAMP WITH TIME ZONE,
    failed_at           TIMESTAMP WITH TIME ZONE,
    priority            INTEGER NOT NULL DEFAULT 5,
    model_used          VARCHAR(200),
    processing_time_ms  BIGINT,
    tokens_used         INTEGER,
    result_summary      TEXT,
    error_message       TEXT,
    error_stack         TEXT,
    attempt_count       INTEGER NOT NULL DEFAULT 0,
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    worker_node         VARCHAR(200),
    metadata            JSONB,

    CONSTRAINT chk_ai_status CHECK (
        status IN ('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED', 'RETRY')
    ),
    CONSTRAINT chk_ai_job_type CHECK (
        job_type IN ('OCR', 'METADATA_EXTRACT', 'SUMMARIZE', 'EMBED',
                     'QA', 'CLASSIFY', 'ENTITIES', 'TRANSLATE', 'FULL_PIPELINE')
    )
);

CREATE INDEX IF NOT EXISTS idx_ai_jobs_doc ON ai_processing_jobs (document_id);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_status ON ai_processing_jobs (status);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_type ON ai_processing_jobs (job_type);
CREATE INDEX IF NOT EXISTS idx_ai_jobs_queue ON ai_processing_jobs (status, priority, queued_at ASC)
    WHERE status IN ('QUEUED', 'RETRY');
CREATE INDEX IF NOT EXISTS idx_ai_jobs_created ON ai_processing_jobs (queued_at DESC);

-- -----------------------------------------------------------------------------
-- EXTRACTED_DOCUMENT_ENTITIES (AI-extracted entities per document)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS extracted_document_entities (
    id              BIGSERIAL PRIMARY KEY,
    document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    entity_type     VARCHAR(100) NOT NULL,
    entity_value    VARCHAR(1000) NOT NULL,
    page_number     INTEGER,
    chunk_start     INTEGER,
    chunk_end       INTEGER,
    confidence      NUMERIC(5,4),
    model           VARCHAR(200),
    extracted_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_extracted_entities_doc ON extracted_document_entities (document_id);
CREATE INDEX IF NOT EXISTS idx_extracted_entities_type ON extracted_document_entities (entity_type);
CREATE INDEX IF NOT EXISTS idx_extracted_entities_value ON extracted_document_entities (entity_value);
CREATE INDEX IF NOT EXISTS idx_extracted_entities_doc_type ON extracted_document_entities (document_id, entity_type);

-- -----------------------------------------------------------------------------
-- DOCUMENT_QUESTION_ANSWERS - Cached Q&A history
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_qa_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID REFERENCES users(id) ON DELETE SET NULL,
    case_id             UUID REFERENCES case_files(id) ON DELETE SET NULL,
    document_ids        UUID[],
    question            TEXT NOT NULL,
    answer              TEXT NOT NULL,
    model_used          VARCHAR(200),
    source_references   JSONB,
    processing_time_ms  BIGINT,
    tokens_used         INTEGER,
    confidence_score    NUMERIC(5,4),
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_qa_history_user ON document_qa_history (user_id);
CREATE INDEX IF NOT EXISTS idx_qa_history_case ON document_qa_history (case_id);
CREATE INDEX IF NOT EXISTS idx_qa_history_created ON document_qa_history (created_at DESC);

-- -----------------------------------------------------------------------------
-- TIMELINE_EVENTS TABLE - Case Investigation Timeline
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS timeline_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id         UUID NOT NULL REFERENCES case_files(id) ON DELETE CASCADE,
    event_type      VARCHAR(100) NOT NULL,
    title           VARCHAR(500) NOT NULL,
    description     TEXT,
    document_id     UUID REFERENCES documents(id) ON DELETE SET NULL,
    evidence_id     UUID REFERENCES evidence(id) ON DELETE SET NULL,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    metadata        JSONB,
    event_date      TIMESTAMP WITH TIME ZONE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_timeline_case ON timeline_events (case_id);
CREATE INDEX IF NOT EXISTS idx_timeline_type ON timeline_events (event_type);
CREATE INDEX IF NOT EXISTS idx_timeline_created_by ON timeline_events (created_by);
CREATE INDEX IF NOT EXISTS idx_timeline_event_date ON timeline_events (event_date DESC);
CREATE INDEX IF NOT EXISTS idx_timeline_created ON timeline_events (created_at DESC);

-- -----------------------------------------------------------------------------
-- EXPORT_REPORTS TABLE - Generated PDF / Case Report exports
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS export_reports (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_type     VARCHAR(100) NOT NULL,
    case_id         UUID REFERENCES case_files(id) ON DELETE SET NULL,
    title           VARCHAR(500) NOT NULL,
    storage_key     VARCHAR(1000) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL DEFAULT 'application/pdf',
    file_size_bytes BIGINT NOT NULL,
    sha256_hash     VARCHAR(64) NOT NULL,
    page_count      INTEGER,
    includes        JSONB,
    generated_by    UUID REFERENCES users(id) ON DELETE SET NULL,
    generated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_export_reports_case ON export_reports (case_id);
CREATE INDEX IF NOT EXISTS idx_export_reports_type ON export_reports (report_type);
CREATE INDEX IF NOT EXISTS idx_export_reports_created_by ON export_reports (generated_by);
CREATE INDEX IF NOT EXISTS idx_export_reports_created ON export_reports (generated_at DESC);

-- -----------------------------------------------------------------------------
-- FINAL: Ensure block-level grants and comments
-- -----------------------------------------------------------------------------
COMMENT ON TABLE ai_processing_jobs IS 'Async AI processing job queue for OCR/summarize/embed/etc.';
COMMENT ON TABLE extracted_document_entities IS 'Persons, organizations, dates, etc. extracted via NER from documents';
COMMENT ON TABLE document_qa_history IS 'Cached historical Q&A sessions for auditing and re-use';
COMMENT ON TABLE timeline_events IS 'Human- and system-generated events on a case investigation timeline';
COMMENT ON TABLE export_reports IS 'Generated case and legal report exports (PDFs)';
