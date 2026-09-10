# DEMS - Digital Evidence Management System

Secure Digital Document Management for Legal & Investigation

## Table of Contents
1. [Overview](#overview)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Features](#features)
5. [Prerequisites](#prerequisites)
6. [Quick Start](#quick-start)
7. [Configuration](#configuration)
8. [API Documentation](#api-documentation)
9. [Development](#development)
10. [Testing](#testing)
11. [Docker Deployment](#docker-deployment)

## Overview

DEMS is a secure, enterprise-grade digital document and evidence management system designed specifically for legal and investigative professionals. It provides end-to-end security, tamper-evident audit trails, and AI-powered document processing capabilities.

## Tech Stack

- **Java 21** - Primary language
- **Spring Boot 3.3.x** - Application framework
- **Spring Security** - Authentication & Authorization (JWT + RBAC)
- **Spring Data JPA** - ORM and data access
- **Spring Validation** - Request validation
- **Spring WebFlux/WebClient** - AI service integration
- **PostgreSQL 16 + pgvector** - Primary database with vector search
- **Redis 7** - Caching and session management
- **MinIO** - Secure object storage for documents
- **Flyway** - Database migrations
- **JWT (JJWT)** - Token-based authentication
- **Argon2id** - Password hashing
- **AES-256-GCM** - Sensitive data encryption
- **SHA-256** - Document integrity verification
- **Swagger/OpenAPI 3.0** - API documentation
- **MinIO** - Object storage
- **ZXing** - QR code generation
- **iText PDF** - Report generation

## Architecture

Clean Architecture with layered separation:

```
┌─────────────────────────────────────────────────┐
│                  Controller Layer               │
│            (REST APIs, DTO Validation)          │
├─────────────────────────────────────────────────┤
│                  Service Layer                  │
│         (Business Logic, Transactions)          │
├─────────────────────────────────────────────────┤
│                 Repository Layer                │
│           (Data Access, JPA Queries)            │
├─────────────────────────────────────────────────┤
│                   Model Layer                   │
│         (Entities, Enums, Value Objects)        │
└─────────────────────────────────────────────────┘
```

### Security Model

**Roles (RBAC):**
- `ADMIN` - Full system access, user management
- `INVESTIGATOR` - Case & evidence management, document upload
- `LEGAL_OFFICER` - Document review, report generation, sharing
- `VIEWER` - Read-only access to assigned resources

**Security Features:**
- JWT access + refresh tokens
- Argon2id password hashing
- AES-256-GCM encryption for sensitive fields
- SHA-256 document integrity hashing
- Tamper-evident audit logs with hash chaining
- Secure authorization checks on every file access

## Features

### Authentication & User Management
- User registration, login, logout
- Token refresh mechanism
- Role-based access control (RBAC)
- User profile management

### Case Management
- Create/update/close cases
- Assign investigators and legal officers
- Case status tracking (DRAFT, ACTIVE, REVIEWED, CLOSED)
- Case priority management
- Case metadata

### Document Management
- Secure document upload with MinIO
- Automatic SHA-256 hashing & duplicate detection
- Version control (never overwrites previous versions)
- Document metadata with JSONB
- Secure download with authorization
- Integrity verification
- AI/OCR processing workflow

### Evidence Management
- Evidence registry & catalog
- Chain of custody tracking
- Evidence transfer logs
- Evidence status management
- Evidence verification

### Audit & Security
- Login/logout audit logs
- Document activity tracking
- Unauthorized access attempt logs
- Tamper-evident hash chain
- Audit chain verification
- Security alerting

### AI Integration (via Python FastAPI)
- OCR processing (PaddleOCR)
- PDF text extraction (PyMuPDF)
- Document summarization (LLM)
- Document Q&A with source/page references
- Semantic search (BGE-M3 embeddings, pgvector)
- Metadata extraction

### Search
- Full-text keyword search
- Advanced filtering
- Semantic similarity search (pgvector)

### Additional Features
- Secure document sharing with expiration
- Investigation timeline events
- Dashboard analytics & metrics
- Relationship graph API (for React Flow)
- QR code evidence verification
- Case/legal report export (PDF)

## Prerequisites

- JDK 21+
- Maven 3.9+
- Docker & Docker Compose (for containerized deployment)
- PostgreSQL 16+ with pgvector extension (for non-Docker)
- Redis 7+ (for non-Docker)
- MinIO (for non-Docker)

## Quick Start

### 1. Configure Environment

```bash
cp .env.example .env
# Edit .env and set your secrets/passwords
```

### 2. Start Infrastructure (Docker)

```bash
docker-compose up -d postgres redis minio
```

### 3. Build & Run Application

```bash
mvn clean package -DskipTests
mvn spring-boot:run
```

### 4. Verify

- Health check: http://localhost:8080/api/health
- Swagger UI: http://localhost:8080/swagger-ui.html

## Configuration

All configuration is externalized via environment variables. See `.env.example` for full list.

### Core Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `SERVER_PORT` | Server port | 8080 |
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | dems_db |
| `DB_USERNAME` | DB user | dems_user |
| `DB_PASSWORD` | DB password | - |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `JWT_SECRET` | JWT signing key (min 32 chars) | - |
| `JWT_ACCESS_TOKEN_EXPIRATION_MS` | Access token TTL | 900000 (15m) |
| `JWT_REFRESH_TOKEN_EXPIRATION_MS` | Refresh token TTL | 604800000 (7d) |
| `MINIO_ENDPOINT` | MinIO server URL | http://localhost:9000 |
| `MINIO_ACCESS_KEY` | MinIO access key | - |
| `MINIO_SECRET_KEY` | MinIO secret key | - |
| `MINIO_BUCKET_NAME` | Default bucket | dems-documents |
| `AES_SECRET_KEY` | AES-256 key (32 bytes) | - |
| `AI_SERVICE_BASE_URL` | Python AI service URL | http://localhost:8000 |

## API Documentation

Access Swagger UI at: `http://localhost:8080/swagger-ui.html`

### Key API Endpoints

#### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login & get tokens
- `POST /api/auth/refresh` - Refresh access token
- `POST /api/auth/logout` - Logout (invalidate tokens)
- `GET /api/auth/me` - Get current user

#### Users
- `GET /api/users` - List users (ADMIN)
- `GET /api/users/{id}` - Get user by ID
- `PUT /api/users/{id}` - Update user (ADMIN)
- `PUT /api/users/{id}/role` - Update role (ADMIN)

#### Cases
- `POST /api/cases` - Create case
- `GET /api/cases` - List cases
- `GET /api/cases/{id}` - Get case details
- `PUT /api/cases/{id}` - Update case
- `POST /api/cases/{id}/assign` - Assign users

#### Documents
- `POST /api/documents/upload` - Upload document
- `GET /api/documents/{id}` - Get document metadata
- `GET /api/documents/{id}/download` - Download document
- `GET /api/documents/{id}/verify` - Verify integrity
- `GET /api/documents/{id}/versions` - List versions

#### Evidence
- `POST /api/evidence` - Register evidence
- `GET /api/evidence` - List evidence
- `POST /api/evidence/{id}/transfer` - Transfer custody

#### Audit
- `GET /api/audit/logs` - Query audit logs
- `GET /api/audit/verify` - Verify audit chain integrity

#### AI
- `POST /api/ai/ocr/{documentId}` - Run OCR
- `POST /api/ai/summarize/{documentId}` - Summarize document
- `POST /api/ai/qa` - Document Q&A
- `POST /api/search/semantic` - Semantic search

## Development

### Project Structure

```
backend/
├── pom.xml
├── .env.example
├── Dockerfile
├── docker-compose.yml
├── README.md
└── src/
    ├── main/
    │   ├── java/com/dems/
    │   │   ├── DemsApplication.java
    │   │   ├── common/           # Exception handling, responses
    │   │   ├── config/           # OpenAPI, other configs
    │   │   ├── health/           # Health check
    │   │   ├── auth/             # Auth controllers & services
    │   │   ├── user/             # User management
    │   │   ├── security/         # JWT, security config, filters
    │   │   ├── casefile/         # Case management
    │   │   ├── document/         # Document management
    │   │   ├── evidence/         # Evidence & chain of custody
    │   │   ├── audit/            # Tamper-evident audit logs
    │   │   ├── integrity/        # Document integrity verification
    │   │   ├── search/           # Keyword + semantic search
    │   │   ├── sharing/          # Secure document sharing
    │   │   ├── timeline/         # Investigation timeline
    │   │   ├── notification/     # Alerts & notifications
    │   │   ├── ai/               # AI service integration
    │   │   ├── storage/          # MinIO storage service
    │   │   ├── crypto/           # AES encryption, hashing
    │   │   └── dashboard/        # Analytics & metrics
    │   └── resources/
    │       ├── application.yml
    │       ├── application-test.yml
    │       └── db/migration/     # Flyway migrations
    └── test/                     # Unit & integration tests
```

### Document Upload Workflow

```
Validate File
    ↓
Check User Authorization
    ↓
Generate SHA-256 Hash
    ↓
Check for Duplicate (hash)
    ↓
Store Securely in MinIO (encrypted)
    ↓
Save Metadata in PostgreSQL
    ↓
Create Version Record
    ↓
Create Audit Event
    ↓
Send Processing Request to AI Service
```

## Testing

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=DemsApplicationTests

# Run with coverage (if JaCoCo configured)
mvn test jacoco:report
```

Tests include:
- Application startup
- Global exception handling
- Health endpoint
- Authentication & JWT
- RBAC enforcement
- Case management
- Document validation & integrity
- Audit log chain verification

## Docker Deployment

Full stack deployment with Docker Compose:

```bash
# 1. Build application JAR
mvn clean package -DskipTests

# 2. Start full stack
docker-compose up -d --build

# 3. View logs
docker-compose logs -f backend

# 4. Stop services
docker-compose down
```

This starts:
- PostgreSQL 16 with pgvector
- Redis 7
- MinIO (with console at :9001)
- Spring Boot backend (port 8080)
