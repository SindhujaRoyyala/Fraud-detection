package com.dems.casefile.repository;

import com.dems.casefile.model.CaseFile;
import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseFileRepository extends JpaRepository<CaseFile, UUID> {

    Optional<CaseFile> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    Page<CaseFile> findByStatusOrderByCreatedAtDesc(CaseStatus status, Pageable pageable);

    Page<CaseFile> findByCreatedByOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<CaseFile> findByAssignedInvestigatorIdOrderByCreatedAtDesc(UUID investigatorId, Pageable pageable);

    Page<CaseFile> findByAssignedLegalOfficerIdOrderByCreatedAtDesc(UUID legalOfficerId, Pageable pageable);

    @Query("SELECT c FROM CaseFile c WHERE c.status = :status AND c.priority = :priority ORDER BY c.createdAt DESC")
    Page<CaseFile> findByStatusAndPriority(
            @Param("status") CaseStatus status,
            @Param("priority") CasePriority priority,
            Pageable pageable);

    @Query("""
        SELECT c FROM CaseFile c WHERE
        (:status IS NULL OR c.status = :status) AND
        (:priority IS NULL OR c.priority = :priority) AND
        (:userId IS NULL OR c.createdBy = :userId OR c.assignedInvestigatorId = :userId OR c.assignedLegalOfficerId = :userId) AND
        (:search IS NULL OR LOWER(c.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(c.description) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY c.createdAt DESC
    """)
    Page<CaseFile> searchCases(
            @Param("status") CaseStatus status,
            @Param("priority") CasePriority priority,
            @Param("userId") UUID userId,
            @Param("search") String search,
            Pageable pageable);
}
