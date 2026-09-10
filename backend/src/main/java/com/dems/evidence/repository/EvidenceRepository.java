package com.dems.evidence.repository;

import com.dems.evidence.model.Evidence;
import com.dems.evidence.model.EvidenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {

    Optional<Evidence> findByEvidenceNumber(String evidenceNumber);

    boolean existsByEvidenceNumber(String evidenceNumber);

    Page<Evidence> findByCaseIdOrderByCreatedAtDesc(UUID caseId, Pageable pageable);

    Page<Evidence> findByCurrentCustodianIdOrderByCreatedAtDesc(UUID custodianId, Pageable pageable);

    Page<Evidence> findByStatusOrderByCreatedAtDesc(EvidenceStatus status, Pageable pageable);

    @Query("""
        SELECT e FROM Evidence e WHERE
        (:caseId IS NULL OR e.caseId = :caseId) AND
        (:status IS NULL OR e.status = :status) AND
        (:custodianId IS NULL OR e.currentCustodianId = :custodianId) AND
        (:search IS NULL OR
            LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(e.evidenceNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY e.createdAt DESC
    """)
    Page<Evidence> searchEvidence(
            @Param("caseId") UUID caseId,
            @Param("status") EvidenceStatus status,
            @Param("custodianId") UUID custodianId,
            @Param("search") String search,
            Pageable pageable);
}
