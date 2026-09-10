package com.dems.evidence.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "evidence_transfers", schema = "public", indexes = {
        @Index(columnList = "evidence_id"),
        @Index(columnList = "from_user_id"),
        @Index(columnList = "to_user_id"),
        @Index(columnList = "created_at")
})
@EntityListeners(AuditingEntityListener.class)
public class EvidenceTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "evidence_id", columnDefinition = "uuid", nullable = false)
    private UUID evidenceId;

    @Column(name = "from_user_id", columnDefinition = "uuid")
    private UUID fromUserId;

    @Column(name = "to_user_id", columnDefinition = "uuid", nullable = false)
    private UUID toUserId;

    @Column(name = "transfer_reason", length = 2000)
    private String transferReason;

    @Column(name = "location", length = 1000)
    private String location;

    @Column(name = "witness_name", length = 255)
    private String witnessName;

    @Column(name = "notes", length = 5000)
    private String notes;

    @Column(name = "previous_chain_hash", length = 64)
    private String previousChainHash;

    @Column(name = "transfer_hash", length = 64, nullable = false)
    private String transferHash;

    @Column(name = "transfer_number", length = 50)
    private String transferNumber;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "receiver_ip", length = 50)
    private String receiverIp;
}
