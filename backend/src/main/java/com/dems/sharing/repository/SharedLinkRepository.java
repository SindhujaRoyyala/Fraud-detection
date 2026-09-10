package com.dems.sharing.repository;

import com.dems.sharing.model.SharedLink;
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
public interface SharedLinkRepository extends JpaRepository<SharedLink, UUID> {

    Optional<SharedLink> findByToken(String token);

    boolean existsByToken(String token);

    Page<SharedLink> findByCreatedByOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<SharedLink> findByDocumentIdOrderByCreatedAtDesc(UUID documentId, Pageable pageable);

    @Query("SELECT s FROM SharedLink s WHERE s.createdBy = :userId AND s.expiresAt > :now " +
            "AND (s.revokedAt IS NULL OR s.revokedAt > :now) ORDER BY s.createdAt DESC")
    Page<SharedLink> findActiveByCreatedBy(@Param("userId") UUID userId, @Param("now") Instant now, Pageable pageable);
}
