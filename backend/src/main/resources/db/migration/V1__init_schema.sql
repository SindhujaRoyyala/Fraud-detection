-- =============================================================================
-- V1__init_schema.sql - DEMS Base Schema Initialization
-- =============================================================================
-- Digital Evidence Management System - Initial PostgreSQL Schema with pgvector
-- =============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
DO $$ BEGIN
    CREATE EXTENSION IF NOT EXISTS "pgvector";
EXCEPTION WHEN OTHERS THEN
    RAISE NOTICE 'pgvector extension not installed; skipping';
END $$;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Ensure we're using UTC timezone for all connections
SET TIME ZONE 'UTC';

-- =============================================================================
-- ENUM TYPES
-- =============================================================================

-- User roles
DO $$ BEGIN
    CREATE TYPE role_enum AS ENUM ('ADMIN', 'INVESTIGATOR', 'LEGAL_OFFICER', 'VIEWER');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Case management enums
DO $$ BEGIN
    CREATE TYPE case_status_enum AS ENUM ('DRAFT', 'ACTIVE', 'REVIEWED', 'CLOSED', 'ARCHIVED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
    CREATE TYPE case_priority_enum AS ENUM ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Document management enums
DO $$ BEGIN
    CREATE TYPE document_status_enum AS ENUM ('UPLOADED', 'PROCESSING', 'PROCESSED', 'PROCESSING_FAILED', 'ARCHIVED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Evidence management enums
DO $$ BEGIN
    CREATE TYPE evidence_status_enum AS ENUM ('COLLECTED', 'IN_CUSTODY', 'TRANSFERRED', 'ANALYZED', 'PRESENTED', 'RETURNED', 'DESTROYED', 'ARCHIVED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Audit event category (all values stored as VARCHAR, enum for reference)
DO $$ BEGIN
    CREATE TYPE audit_event_enum AS ENUM (
        'USER_REGISTERED', 'USER_LOGIN', 'USER_LOGIN_FAILED', 'USER_LOGOUT',
        'USER_TOKEN_REFRESHED', 'USER_UPDATED', 'USER_ROLE_CHANGED',
        'USER_PASSWORD_CHANGED', 'USER_DEACTIVATED',
        'CASE_CREATED', 'CASE_UPDATED', 'CASE_STATUS_CHANGED', 'CASE_ASSIGNED',
        'DOCUMENT_UPLOADED', 'DOCUMENT_DOWNLOADED', 'DOCUMENT_VERSIONED',
        'DOCUMENT_DELETED', 'DOCUMENT_INTEGRITY_VERIFIED',
        'EVIDENCE_REGISTERED', 'EVIDENCE_TRANSFERRED', 'EVIDENCE_VERIFIED',
        'UNAUTHORIZED_ACCESS_ATTEMPT',
        'SHARING_LINK_CREATED', 'SHARING_LINK_ACCESSED', 'SHARING_LINK_EXPIRED',
        'AI_PROCESSING_STARTED', 'AI_PROCESSING_COMPLETED', 'AI_PROCESSING_FAILED',
        'REPORT_EXPORTED', 'AUDIT_CHAIN_VERIFIED', 'SECURITY_ALERT_TRIGGERED'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Timeline event types
DO $$ BEGIN
    CREATE TYPE timeline_event_enum AS ENUM (
        'CASE_CREATED', 'CASE_STATUS_CHANGED', 'CASE_ASSIGNED',
        'DOCUMENT_UPLOADED', 'DOCUMENT_VERSIONED', 'DOCUMENT_PROCESSED',
        'EVIDENCE_REGISTERED', 'EVIDENCE_TRANSFERRED',
        'USER_LOGIN', 'AUDIT_ALERT', 'COMMENT_ADDED',
        'TASK_COMPLETED', 'REPORT_GENERATED', 'SHARE_CREATED',
        'AI_PROCESSING_COMPLETED'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- Notification types
DO $$ BEGIN
    CREATE TYPE notification_type_enum AS ENUM (
        'CASE_ASSIGNED', 'DOCUMENT_SHARED', 'EVIDENCE_TRANSFERRED',
        'EVIDENCE_CUSTODY_ALERT', 'AUDIT_ALERT',
        'AI_PROCESSING_COMPLETE', 'AI_PROCESSING_FAILED',
        'CASE_STATUS_CHANGED', 'SECURITY_ALERT',
        'USER_MENTION', 'TASK_ASSIGNED', 'REPORT_GENERATED',
        'SHARE_LINK_EXPIRING', 'UNAUTHORIZED_ACCESS_ATTEMPT'
    );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- =============================================================================
-- BASE TABLE: USERS (defined in V2 separately with auth details)
-- BASE TABLE PLACEHOLDERS: Actual DDL in subsequent migrations per module
-- =============================================================================

-- V1 placeholder note: All concrete tables defined in V2-V8 below in follow-up files
-- This file creates extensions, enums, and sets foundational configuration.

-- =============================================================================
-- FUNCTIONS
-- =============================================================================

-- Trigger function to auto-update updated_at columns
CREATE OR REPLACE FUNCTION trigger_set_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Helper function to generate UUID v7-style values (time-ordered)
CREATE OR REPLACE FUNCTION generate_time_ordered_uuid()
RETURNS uuid AS $$
BEGIN
    RETURN uuid_generate_v4();
END;
$$ LANGUAGE plpgsql;

COMMENT ON DATABASE current_database IS 'DEMS - Digital Evidence Management System Database';
