-- =============================================================================
-- V4__document_management.sql - Document Management, Storage & Version Control
-- =============================================================================

-- -----------------------------------------------------------------------------
-- DOCUMENTS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id             UUID REFERENCES case_files(id) ON DELETE SET NULL,
    original_filename   VARCHAR(500) NOT NULL,
    storage_key         VARCHAR(1000) NOT NULL,
    mime_type           VARCHAR(200),
    file_size_bytes     BIGINT NOT NULL,
    sha256_hash         VARCHAR(64) NOT NULL,
    status              VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
    title               VARCHAR(500),
    description         TEXT,
    extracted_text      TEXT,
    extracted_metadata  JSONB,
    summary             TEXT,
    current_version     INTEGER NOT NULL DEFAULT 1,
    ocr_completed       BOOLEAN NOT NULL DEFAULT FALSE,
    embedding_stored    BOOLEAN NOT NULL DEFAULT FALSE,
    sensitive           BOOLEAN NOT NULL DEFAULT FALSE,
    document_tags       VARCHAR(2000),
    classification      VARCHAR(100),
    uploaded_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT uq_documents_sha256 UNIQUE (sha256_hash),
    CONSTRAINT uq_documents_storage_key UNIQUE (storage_key),
    CONSTRAINT chk_documents_status CHECK (
        status IN ('UPLOADED', 'PROCESSING', 'PROCESSED', 'PROCESSING_FAILED', 'ARCHIVED')
    )
);

CREATE INDEX IF NOT EXISTS idx_documents_case_id ON documents (case_id);
CREATE INDEX IF NOT EXISTS idx_documents_uploaded_by ON documents (uploaded_by);
CREATE INDEX IF NOT EXISTS idx_documents_sha256_hash ON documents (sha256_hash);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents (status);
CREATE INDEX IF NOT EXISTS idx_documents_mime_type ON documents (mime_type);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_documents_classification ON documents (classification);
CREATE INDEX IF NOT EXISTS idx_documents_sensitive ON documents (sensitive) WHERE sensitive = TRUE;
CREATE INDEX IF NOT EXISTS idx_documents_ftsearch ON documents USING GIN (
    to_tsvector('english',
        COALESCE(title, '') || ' ' ||
        COALESCE(description, '') || ' ' ||
        COALESCE(original_filename, '') || ' ' ||
        COALESCE(extracted_text, '')
    )
);

DROP TRIGGER IF EXISTS trg_documents_update_timestamp ON documents;
CREATE TRIGGER trg_documents_update_timestamp
    BEFORE UPDATE ON documents
    FOR EACH ROW EXECUTE FUNCTION trigger_set_timestamp();

-- -----------------------------------------------------------------------------
-- DOCUMENT_VERSIONS TABLE
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS document_versions (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id         UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    version_number      INTEGER NOT NULL,
    original_filename   VARCHAR(500),
    storage_key         VARCHAR(1000) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    sha256_hash         VARCHAR(64) NOT NULL,
    change_description  TEXT,
    mime_type           VARCHAR(200),
    created_by          UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_doc_versions_doc_ver UNIQUE (document_id, version_number)
);

CREATE INDEX IF NOT EXISTS idx_doc_versions_document_id ON document_versions (document_id);
CREATE INDEX IF NOT EXISTS idx_doc_versions_version ON document_versions (document_id, version_number DESC);
CREATE INDEX IF NOT EXISTS idx_doc_versions_sha256 ON document_versions (sha256_hash);
CREATE INDEX IF NOT EXISTS idx_doc_versions_created ON document_versions (created_at DESC);

-- -----------------------------------------------------------------------------
-- DOCUMENT EMBEDDINGS (pgvector) for semantic search
-- -----------------------------------------------------------------------------
DO $$ 
BEGIN
    IF EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
        CREATE TABLE IF NOT EXISTS document_embeddings (
            id              BIGSERIAL PRIMARY KEY,
            document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
            chunk_number    INTEGER NOT NULL,
            page_number     INTEGER,
            chunk_text      TEXT NOT NULL,
            embedding       vector(1024),
            created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_doc_embeddings_doc_chunk UNIQUE (document_id, chunk_number)
        );
        CREATE INDEX IF NOT EXISTS idx_doc_embeddings_document_id ON document_embeddings (document_id);
        CREATE INDEX IF NOT EXISTS idx_doc_embeddings_hnsw ON document_embeddings
            USING hnsw (embedding vector_cosine_ops) WITH (m = 16, ef_construction = 200);
    ELSE
        CREATE TABLE IF NOT EXISTS document_embeddings (
            id              BIGSERIAL PRIMARY KEY,
            document_id     UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
            chunk_number    INTEGER NOT NULL,
            page_number     INTEGER,
            chunk_text      TEXT NOT NULL,
            embedding       TEXT,
            created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT uq_doc_embeddings_doc_chunk UNIQUE (document_id, chunk_number)
        );
        CREATE INDEX IF NOT EXISTS idx_doc_embeddings_document_id ON document_embeddings (document_id);
    END IF;
END $$;

COMMENT ON TABLE documents IS 'Primary document metadata; actual files stored in MinIO';
COMMENT ON TABLE document_versions IS 'Immutable version history for each document';
COMMENT ON TABLE document_embeddings IS 'Vector embeddings per document chunk (BGE-M3 dim=1024) used for semantic search';
