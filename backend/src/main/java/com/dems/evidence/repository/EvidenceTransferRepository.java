package com.dems.evidence.repository;

import com.dems.evidence.model.EvidenceTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceTransferRepository extends JpaRepository<EvidenceTransfer, UUID> {

    List<EvidenceTransfer> findByEvidenceIdOrderByCreatedAtAsc(UUID evidenceId);

    List<EvidenceTransfer> findByEvidenceIdOrderByCreatedAtDesc(UUID evidenceId);

    Optional<EvidenceTransfer> findFirstByEvidenceIdOrderByCreatedAtDesc(UUID evidenceId);
}
