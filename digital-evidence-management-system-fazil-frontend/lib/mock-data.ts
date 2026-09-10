import type { User, Document, Case, Evidence, AuditLog, DashboardStats } from "./types";

export const mockUsers: User[] = [
  { id: "u1", name: "Sarah Mitchell", email: "sarah.mitchell@agency.gov", role: "admin", department: "Administration", status: "active", createdAt: "2024-01-15", lastLogin: "2026-09-07T14:30:00", twoFactorEnabled: true },
  { id: "u2", name: "James Rodriguez", email: "james.r@agency.gov", role: "investigator", department: "Investigations", status: "active", createdAt: "2024-03-22", lastLogin: "2026-09-07T11:15:00", twoFactorEnabled: true },
  { id: "u3", name: "Emily Chen", email: "emily.c@agency.gov", role: "analyst", department: "Intelligence", status: "active", createdAt: "2024-06-01", lastLogin: "2026-09-06T16:45:00", twoFactorEnabled: false },
  { id: "u4", name: "Michael Brown", email: "michael.b@agency.gov", role: "investigator", department: "Investigations", status: "active", createdAt: "2024-02-10", lastLogin: "2026-09-07T09:00:00", twoFactorEnabled: true },
  { id: "u5", name: "Lisa Anderson", email: "lisa.a@agency.gov", role: "viewer", department: "Legal", status: "active", createdAt: "2025-01-05", lastLogin: "2026-09-05T13:20:00", twoFactorEnabled: false },
  { id: "u6", name: "David Kim", email: "david.k@agency.gov", role: "analyst", department: "Cybersecurity", status: "active", createdAt: "2024-08-15", lastLogin: "2026-09-07T10:00:00", twoFactorEnabled: true },
  { id: "u7", name: "Rachel Green", email: "rachel.g@agency.gov", role: "investigator", department: "Investigations", status: "inactive", createdAt: "2024-04-20", lastLogin: "2026-08-15T08:30:00", twoFactorEnabled: true },
  { id: "u8", name: "Tom Wilson", email: "tom.w@agency.gov", role: "admin", department: "IT Security", status: "active", createdAt: "2023-11-01", lastLogin: "2026-09-07T15:00:00", twoFactorEnabled: true },
];

export const mockDocuments: Document[] = [
  { id: "d1", title: "Case Alpha - Initial Report", type: "pdf", category: "report", caseId: "c1", uploadedBy: "Sarah Mitchell", uploadedAt: "2026-09-01T10:00:00", size: 2457600, status: "active", encrypted: true, hash: "sha256:a1b2c3d4e5f6", tags: ["urgent", "classified"], classification: "confidential" },
  { id: "d2", title: "Financial Records Q3 2026", type: "xlsx", category: "evidence", caseId: "c2", uploadedBy: "James Rodriguez", uploadedAt: "2026-09-03T14:30:00", size: 1048576, status: "active", encrypted: true, hash: "sha256:f6e5d4c3b2a1", tags: ["financial", "quarterly"], classification: "restricted" },
  { id: "d3", title: "Witness Statement - J. Doe", type: "docx", category: "evidence", caseId: "c1", uploadedBy: "Emily Chen", uploadedAt: "2026-09-05T09:15:00", size: 524288, status: "active", encrypted: true, hash: "sha256:1a2b3c4d5e6f", tags: ["witness", "statement"], classification: "confidential" },
  { id: "d4", title: "Surveillance Photo Set", type: "image", category: "evidence", caseId: "c3", uploadedBy: "Michael Brown", uploadedAt: "2026-09-04T16:00:00", size: 15728640, status: "active", encrypted: true, hash: "sha256:6f5e4d3c2b1a", tags: ["surveillance", "photos"], classification: "restricted" },
  { id: "d5", title: "Legal Brief - Motion to Compel", type: "pdf", category: "legal", caseId: "c2", uploadedBy: "Lisa Anderson", uploadedAt: "2026-09-02T11:45:00", size: 3145728, status: "active", encrypted: true, hash: "sha256:abcdef123456", tags: ["legal", "motion"], classification: "internal" },
  { id: "d6", title: "Communication Logs Extract", type: "pdf", category: "evidence", caseId: "c4", uploadedBy: "David Kim", uploadedAt: "2026-09-06T13:20:00", size: 786432, status: "pending", encrypted: true, hash: "sha256:654321fedcba", tags: ["communications", "extract"], classification: "confidential" },
  { id: "d7", title: "Incident Response Plan v2", type: "docx", category: "internal", uploadedBy: "Tom Wilson", uploadedAt: "2026-08-20T10:00:00", size: 2097152, status: "active", encrypted: true, hash: "sha256:deadbeef1234", tags: ["policy", "security"], classification: "internal" },
  { id: "d8", title: "Audio Recording - Interview", type: "audio", category: "evidence", caseId: "c3", uploadedBy: "James Rodriguez", uploadedAt: "2026-09-07T08:00:00", size: 52428800, status: "active", encrypted: true, hash: "sha256:cafebabe5678", tags: ["audio", "interview"], classification: "restricted" },
  { id: "d9", title: "Network Forensics Report", type: "pdf", category: "report", caseId: "c4", uploadedBy: "David Kim", uploadedAt: "2026-09-05T15:30:00", size: 4194304, status: "active", encrypted: true, hash: "sha256:face0ff0dead", tags: ["forensics", "network"], classification: "confidential" },
  { id: "d10", title: "Compliance Checklist 2026", type: "xlsx", category: "internal", uploadedBy: "Sarah Mitchell", uploadedAt: "2026-07-15T09:00:00", size: 262144, status: "archived", encrypted: false, hash: "sha256:123456789abc", tags: ["compliance", "checklist"], classification: "public" },
];

export const mockCases: Case[] = [
  { id: "c1", title: "Operation Shadow Network", description: "Investigation into organized cyber intrusion targeting government agencies", status: "under_investigation", priority: "critical", assignee: "u2", assigneeName: "James Rodriguez", createdAt: "2026-08-15T08:00:00", updatedAt: "2026-09-07T14:30:00", documentsCount: 12, evidenceCount: 8, tags: ["cyber", "organized", "critical"], category: "Cybercrime" },
  { id: "c2", title: "Financial Fraud Investigation", description: "Suspected embezzlement and money laundering through shell companies", status: "under_investigation", priority: "high", assignee: "u4", assigneeName: "Michael Brown", createdAt: "2026-07-01T10:00:00", updatedAt: "2026-09-06T11:00:00", documentsCount: 23, evidenceCount: 15, tags: ["financial", "fraud"], category: "Financial Crimes" },
  { id: "c3", title: "Missing Persons Case - Lake District", description: "Multiple missing persons reports in the Lake District area", status: "open", priority: "medium", assignee: "u2", assigneeName: "James Rodriguez", createdAt: "2026-08-20T12:00:00", updatedAt: "2026-09-05T16:00:00", documentsCount: 7, evidenceCount: 5, tags: ["missing persons", "priority"], category: "General Investigation" },
  { id: "c4", title: "Data Breach Response - TechCorp", description: "Large-scale data breach at TechCorp Inc., customer data potentially compromised", status: "open", priority: "high", assignee: "u6", assigneeName: "David Kim", createdAt: "2026-09-01T09:00:00", updatedAt: "2026-09-07T10:00:00", documentsCount: 9, evidenceCount: 4, tags: ["data breach", "cyber"], category: "Cybercrime" },
  { id: "c5", title: "Internal Affairs Review", description: "Internal review of policy compliance and procedural adherence", status: "closed", priority: "low", assignee: "u8", assigneeName: "Tom Wilson", createdAt: "2026-06-01T08:00:00", updatedAt: "2026-08-30T17:00:00", documentsCount: 15, evidenceCount: 2, tags: ["internal", "compliance"], category: "Internal Affairs" },
  { id: "c6", title: "Counter-Terrorism Intelligence", description: "Ongoing intelligence gathering related to domestic threat assessment", status: "pending_review", priority: "critical", assignee: "u3", assigneeName: "Emily Chen", createdAt: "2026-05-10T07:00:00", updatedAt: "2026-09-04T12:00:00", documentsCount: 42, evidenceCount: 28, tags: ["terrorism", "intelligence", "classified"], category: "National Security" },
];

export const mockEvidence: Evidence[] = [
  { id: "e1", title: "Seized Hard Drive - Dell XPS", type: "digital", caseId: "c1", caseTitle: "Operation Shadow Network", collectedBy: "James Rodriguez", collectedAt: "2026-08-20T14:00:00", status: "stored", encrypted: true, hash: "sha256:evidence001", description: "Primary suspect's workstation hard drive containing potential malware artifacts", chainOfCustody: [
    { action: "Collected", by: "James Rodriguez", at: "2026-08-20T14:00:00", notes: "Seized from suspect's office" },
    { action: "Transferred to Lab", by: "James Rodriguez", at: "2026-08-20T16:30:00", notes: "Submitted for forensic analysis" },
    { action: "Analysis Complete", by: "David Kim", at: "2026-08-25T10:00:00", notes: "Forensic image created, original sealed" },
  ]},
  { id: "e2", title: "Financial Transaction Records", type: "documentary", caseId: "c2", caseTitle: "Financial Fraud Investigation", collectedBy: "Michael Brown", collectedAt: "2026-07-10T09:00:00", status: "under_review", encrypted: true, hash: "sha256:evidence002", description: "Bank statements and wire transfer records from 3 shell companies", chainOfCustody: [
    { action: "Obtained via Subpoena", by: "Michael Brown", at: "2026-07-10T09:00:00", notes: "Received from First National Bank" },
    { action: "Catalogued", by: "Lisa Anderson", at: "2026-07-12T11:00:00", notes: "Documents scanned and indexed" },
  ]},
  { id: "e3", title: "DNA Sample Kit", type: "physical", caseId: "c3", caseTitle: "Missing Persons Case - Lake District", collectedBy: "Emily Chen", collectedAt: "2026-08-25T08:00:00", status: "stored", encrypted: false, description: "DNA samples from last known location for comparison analysis", chainOfCustody: [
    { action: "Collected", by: "Emily Chen", at: "2026-08-25T08:00:00", notes: "Collected from scene, properly packaged" },
    { action: "Transferred to Lab", by: "Emily Chen", at: "2026-08-25T12:00:00", notes: "Sent to state forensics lab" },
  ]},
  { id: "e4", title: "Network Traffic Capture", type: "digital", caseId: "c4", caseTitle: "Data Breach Response - TechCorp", collectedBy: "David Kim", collectedAt: "2026-09-02T15:00:00", status: "stored", encrypted: true, hash: "sha256:evidence004", description: "72-hour network packet capture from affected server segment", chainOfCustody: [
    { action: "Captured", by: "David Kim", at: "2026-09-02T15:00:00", notes: "Network mirror port capture initiated" },
    { action: "Hashed & Sealed", by: "David Kim", at: "2026-09-03T09:00:00", notes: "SHA-256 verified, stored on encrypted volume" },
  ]},
  { id: "e5", title: "CCTV Footage - Main Lobby", type: "digital", caseId: "c3", caseTitle: "Missing Persons Case - Lake District", collectedBy: "James Rodriguez", collectedAt: "2026-08-22T10:00:00", status: "under_review", encrypted: true, hash: "sha256:evidence005", description: "Security camera footage from building lobby covering relevant time period", chainOfCustody: [
    { action: "Obtained", by: "James Rodriguez", at: "2026-08-22T10:00:00", notes: "Extracted from building security system" },
  ]},
];

export const mockAuditLogs: AuditLog[] = [
  { id: "a1", userId: "u1", userName: "Sarah Mitchell", action: "Login", resource: "System", resourceId: "-", details: "Successful login via 2FA", timestamp: "2026-09-07T14:30:00", ipAddress: "192.168.1.100", status: "success", category: "authentication" },
  { id: "a2", userId: "u2", userName: "James Rodriguez", action: "View Document", resource: "Document", resourceId: "d1", details: "Viewed 'Case Alpha - Initial Report'", timestamp: "2026-09-07T14:25:00", ipAddress: "192.168.1.105", status: "success", category: "document" },
  { id: "a3", userId: "u6", userName: "David Kim", action: "Upload Evidence", resource: "Evidence", resourceId: "e4", details: "Uploaded network traffic capture for case c4", timestamp: "2026-09-07T13:15:00", ipAddress: "192.168.1.112", status: "success", category: "document" },
  { id: "a4", userId: "u3", userName: "Emily Chen", action: "Create Case", resource: "Case", resourceId: "c6", details: "Created new case 'Counter-Terrorism Intelligence'", timestamp: "2026-09-07T12:00:00", ipAddress: "192.168.1.108", status: "success", category: "case" },
  { id: "a5", userId: "u7", userName: "Rachel Green", action: "Failed Login", resource: "System", resourceId: "-", details: "Invalid password attempt (3rd attempt)", timestamp: "2026-09-07T11:45:00", ipAddress: "203.0.113.50", status: "failure", category: "security" },
  { id: "a6", userId: "u8", userName: "Tom Wilson", action: "Update Settings", resource: "System", resourceId: "-", details: "Changed encryption policy to AES-256-GCM", timestamp: "2026-09-07T11:00:00", ipAddress: "192.168.1.101", status: "success", category: "system" },
  { id: "a7", userId: "u4", userName: "Michael Brown", action: "Download Report", resource: "Document", resourceId: "d2", details: "Downloaded 'Financial Records Q3 2026'", timestamp: "2026-09-07T10:30:00", ipAddress: "192.168.1.110", status: "success", category: "document" },
  { id: "a8", userId: "u5", userName: "Lisa Anderson", action: "Access Denied", resource: "Document", resourceId: "d9", details: "Attempted to access restricted network forensics report", timestamp: "2026-09-07T10:15:00", ipAddress: "192.168.1.115", status: "failure", category: "security" },
  { id: "a9", userId: "u1", userName: "Sarah Mitchell", action: "Add User", resource: "User", resourceId: "u9", details: "Created new user account for intern", timestamp: "2026-09-07T09:30:00", ipAddress: "192.168.1.100", status: "success", category: "user" },
  { id: "a10", userId: "u2", userName: "James Rodriguez", action: "Update Case", resource: "Case", resourceId: "c1", details: "Updated case status to 'under_investigation'", timestamp: "2026-09-07T09:00:00", ipAddress: "192.168.1.105", status: "success", category: "case" },
  { id: "a11", userId: "u6", userName: "David Kim", action: "Verify Hash", resource: "Evidence", resourceId: "e4", details: "Integrity verification passed for network capture", timestamp: "2026-09-07T08:45:00", ipAddress: "192.168.1.112", status: "success", category: "security" },
  { id: "a12", userId: "u8", userName: "Tom Wilson", action: "System Backup", resource: "System", resourceId: "-", details: "Automated daily backup completed successfully", timestamp: "2026-09-07T03:00:00", ipAddress: "10.0.0.1", status: "success", category: "system" },
];

export const mockDashboardStats: DashboardStats = {
  totalDocuments: 1247,
  totalCases: 48,
  activeUsers: 32,
  pendingReviews: 12,
  documentsThisWeek: 89,
  casesThisMonth: 7,
  securityAlerts: 3,
  storageUsed: 156.8,
};

export const documentActivityData = [
  { name: "Mon", uploads: 12, downloads: 34, views: 89 },
  { name: "Tue", uploads: 19, downloads: 28, views: 102 },
  { name: "Wed", uploads: 8, downloads: 45, views: 76 },
  { name: "Thu", uploads: 24, downloads: 31, views: 115 },
  { name: "Fri", uploads: 15, downloads: 42, views: 94 },
  { name: "Sat", uploads: 3, downloads: 8, views: 23 },
  { name: "Sun", uploads: 2, downloads: 5, views: 18 },
];

export const documentTypeData = [
  { name: "PDF", value: 485, color: "#3b82f6" },
  { name: "DOCX", value: 312, color: "#10b981" },
  { name: "XLSX", value: 198, color: "#f59e0b" },
  { name: "Images", value: 143, color: "#8b5cf6" },
  { name: "Audio", value: 56, color: "#ef4444" },
  { name: "Video", value: 34, color: "#06b6d4" },
  { name: "Other", value: 19, color: "#6b7280" },
];

export const caseStatisticsData = [
  { name: "Open", count: 8, color: "#3b82f6" },
  { name: "Investigating", count: 15, color: "#f59e0b" },
  { name: "Pending Review", count: 5, color: "#8b5cf6" },
  { name: "Closed", count: 18, color: "#10b981" },
  { name: "Archived", count: 2, color: "#6b7280" },
];

export const securityActivityData = [
  { month: "Jan", authentications: 1200, failedAttempts: 12, securityEvents: 3 },
  { month: "Feb", authentications: 1100, failedAttempts: 8, securityEvents: 1 },
  { month: "Mar", authentications: 1350, failedAttempts: 15, securityEvents: 5 },
  { month: "Apr", authentications: 1280, failedAttempts: 6, securityEvents: 2 },
  { month: "May", authentications: 1420, failedAttempts: 11, securityEvents: 4 },
  { month: "Jun", authentications: 1380, failedAttempts: 9, securityEvents: 2 },
  { month: "Jul", authentications: 1500, failedAttempts: 7, securityEvents: 1 },
  { month: "Aug", authentications: 1450, failedAttempts: 13, securityEvents: 6 },
  { month: "Sep", authentications: 920, failedAttempts: 5, securityEvents: 3 },
];

export const auditStatisticsData = [
  { name: "Authentication", count: 342, color: "#3b82f6" },
  { name: "Document", count: 567, color: "#10b981" },
  { name: "Case", count: 234, color: "#f59e0b" },
  { name: "User", count: 89, color: "#8b5cf6" },
  { name: "System", count: 156, color: "#06b6d4" },
  { name: "Security", count: 45, color: "#ef4444" },
];
