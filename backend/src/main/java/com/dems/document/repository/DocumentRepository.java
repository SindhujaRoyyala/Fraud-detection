package com.dems.document.repository;

import com.dems.document.model.Document;
import com.dems.document.model.DocumentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findBySha256Hash(String sha256Hash);

    boolean existsBySha256Hash(String sha256Hash);

    Page<Document> findByCaseIdOrderByCreatedAtDesc(UUID caseId, Pageable pageable);

    Page<Document> findByUploadedByOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<Document> findByStatusOrderByCreatedAtDesc(DocumentStatus status, Pageable pageable);

    List<Document> findByStatusAndEmbeddingStoredFalse(DocumentStatus status);

    @Query("""
        SELECT d FROM Document d WHERE
        d.caseId = :caseId AND
        (:status IS NULL OR d.status = :status) AND
        (:search IS NULL OR
            LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY d.createdAt DESC
    """)
    Page<Document> searchInCase(
            @Param("caseId") UUID caseId,
            @Param("status") DocumentStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
        SELECT d FROM Document d WHERE
        (:caseId IS NULL OR d.caseId = :caseId) AND
        (:status IS NULL OR d.status = :status) AND
        (:uploadedBy IS NULL OR d.uploadedBy = :uploadedBy) AND
        (:search IS NULL OR
            LOWER(d.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(d.originalFilename) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(d.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY d.createdAt DESC
    """)
    Page<Document> searchAll(
            @Param("caseId") UUID caseId,
            @Param("status") DocumentStatus status,
            @Param("uploadedBy") UUID uploadedBy,
            @Param("search") String search,
            Pageable pageable);
}
