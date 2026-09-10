package com.dems.audit.repository;

import com.dems.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Optional<AuditLog> findFirstByOrderByBlockNumberDesc();

    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<AuditLog> findByCaseIdOrderByCreatedAtDesc(UUID caseId, Pageable pageable);

    Page<AuditLog> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);

    Page<AuditLog> findByEventOrderByCreatedAtDesc(String event, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable);

    @Query("SELECT a FROM AuditLog a ORDER BY a.blockNumber ASC")
    java.util.List<AuditLog> findAllOrderedByBlock();

    boolean existsByEntryHash(String entryHash);
}
