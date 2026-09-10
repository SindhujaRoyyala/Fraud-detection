package com.dems.document.controller;

import com.dems.common.response.ApiResponse;
import com.dems.document.dto.*;
import com.dems.document.model.DocumentStatus;
import com.dems.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Document upload, download, metadata & version management APIs")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Secure document upload with SHA-256 hashing and duplicate detection")
    public ResponseEntity<ApiResponse<UploadResult>> uploadDocument(
            @Parameter(description = "File to upload") @RequestParam("file") MultipartFile file,
            @Parameter(description = "Associated case ID") @RequestParam(required = false) UUID caseId,
            @Parameter(description = "Document title") @RequestParam(required = false) String title,
            @Parameter(description = "Document description") @RequestParam(required = false) String description,
            @Parameter(description = "Tags (comma separated)") @RequestParam(required = false) String tags,
            @Parameter(description = "Contains sensitive info") @RequestParam(required = false) Boolean sensitive,
            @Parameter(description = "Classification level") @RequestParam(required = false) String classification) {
        UploadResult result = documentService.uploadDocument(
                file, caseId, title, description, tags, sensitive, classification);
        return ResponseEntity.ok(ApiResponse.created("Document uploaded successfully", result));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload new version", description = "Create a new version of an existing document (never overwrites)")
    public ResponseEntity<ApiResponse<UploadResult>> uploadNewVersion(
            @PathVariable UUID documentId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String changeDescription) {
        UploadResult result = documentService.uploadNewVersion(documentId, file, changeDescription);
        return ResponseEntity.ok(ApiResponse.created("Document version created", result));
    }

    @GetMapping
    @Operation(summary = "List documents", description = "Paginated list of documents with filters (role-scoped)")
    public ResponseEntity<ApiResponse<Page<DocumentResponse>>> getDocuments(
            @Parameter(description = "Filter by case ID") @RequestParam(required = false) UUID caseId,
            @Parameter(description = "Filter by status") @RequestParam(required = false) DocumentStatus status,
            @Parameter(description = "Filter by uploader user ID") @RequestParam(required = false) UUID uploadedBy,
            @Parameter(description = "Search by title/filename/description") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocuments(caseId, status, uploadedBy, search, pageable)));
    }

    @GetMapping("/{documentId}")
    @Operation(summary = "Get document metadata", description = "View document metadata (requires authorization)")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocumentById(documentId)));
    }

    @GetMapping("/{documentId}/download")
    @Operation(summary = "Download document", description = "Download document file (authorized access + audit logged)")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID documentId,
            @Parameter(description = "Specific version number (optional)") @RequestParam(required = false) Integer version) {
        return documentService.downloadDocument(documentId, version);
    }

    @GetMapping("/{documentId}/versions")
    @Operation(summary = "List document versions", description = "Complete version history of a document")
    public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> getDocumentVersions(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocumentVersions(documentId)));
    }

    @GetMapping("/{documentId}/verify")
    @Operation(summary = "Verify document integrity", description = "Re-compute SHA-256 hash and compare with stored value")
    public ResponseEntity<ApiResponse<IntegrityVerificationResult>> verifyIntegrity(@PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.verifyIntegrity(documentId)));
    }

    @PutMapping("/{documentId}")
    @Operation(summary = "Update document metadata", description = "Update title, description, tags, classification, status")
    public ResponseEntity<ApiResponse<DocumentResponse>> updateDocument(
            @PathVariable UUID documentId,
            @Valid @RequestBody UpdateDocumentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Document updated", documentService.updateDocument(documentId, request)));
    }
}
