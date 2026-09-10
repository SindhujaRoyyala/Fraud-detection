package com.dems.casefile;

import com.dems.casefile.model.CasePriority;
import com.dems.casefile.model.CaseStatus;
import com.dems.casefile.model.CaseFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CaseManagementTest {

    @Test
    @DisplayName("CaseFile entity: builder produces correct values")
    void caseFile_Builder_AllFields() {
        UUID id = UUID.randomUUID();
        UUID creator = UUID.randomUUID();
        UUID inv = UUID.randomUUID();
        UUID legal = UUID.randomUUID();
        Instant now = Instant.now();

        CaseFile c = CaseFile.builder()
                .id(id)
                .caseNumber("CASE-2025-00001")
                .title("Test Fraud Case")
                .description("Description here")
                .status(CaseStatus.ACTIVE)
                .priority(CasePriority.HIGH)
                .caseType("FRAUD")
                .jurisdiction("NY, USA")
                .assignedInvestigatorId(inv)
                .assignedLegalOfficerId(legal)
                .openedAt(now)
                .createdBy(creator)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(id, c.getId());
        assertEquals("CASE-2025-00001", c.getCaseNumber());
        assertEquals("Test Fraud Case", c.getTitle());
        assertEquals(CaseStatus.ACTIVE, c.getStatus());
        assertEquals(CasePriority.HIGH, c.getPriority());
        assertEquals(inv, c.getAssignedInvestigatorId());
        assertEquals(legal, c.getAssignedLegalOfficerId());
    }

    @Test
    @DisplayName("CaseFile defaults: DRAFT & MEDIUM")
    void caseFile_DefaultValues() {
        CaseFile c = CaseFile.builder().title("X").caseNumber("C").build();

        assertEquals(CaseStatus.DRAFT, c.getStatus());
        assertEquals(CasePriority.MEDIUM, c.getPriority());
        assertNull(c.getCreatedAt());
    }

    @Test
    @DisplayName("CaseStatus enum has all expected values")
    void caseStatus_AllValuesExist() {
        CaseStatus[] values = CaseStatus.values();
        assertEquals(5, values.length);
        assertNotNull(CaseStatus.valueOf("DRAFT"));
        assertNotNull(CaseStatus.valueOf("ACTIVE"));
        assertNotNull(CaseStatus.valueOf("REVIEWED"));
        assertNotNull(CaseStatus.valueOf("CLOSED"));
        assertNotNull(CaseStatus.valueOf("ARCHIVED"));
    }

    @Test
    @DisplayName("CasePriority enum ordering")
    void casePriority_Ordering() {
        assertEquals(0, CasePriority.LOW.ordinal());
        assertEquals(1, CasePriority.MEDIUM.ordinal());
        assertEquals(2, CasePriority.HIGH.ordinal());
        assertEquals(3, CasePriority.CRITICAL.ordinal());
    }
}
