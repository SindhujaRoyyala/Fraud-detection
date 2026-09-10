export interface User {
  id: string;
  name: string;
  email: string;
  role: "admin" | "investigator" | "analyst" | "viewer";
  avatar?: string;
  department: string;
  status: "active" | "inactive" | "suspended";
  createdAt: string;
  lastLogin: string;
  twoFactorEnabled: boolean;
}

export interface Document {
  id: string;
  title: string;
  type: "pdf" | "docx" | "xlsx" | "image" | "video" | "audio" | "other";
  category: "evidence" | "report" | "correspondence" | "legal" | "internal";
  caseId?: string;
  uploadedBy: string;
  uploadedAt: string;
  size: number;
  status: "active" | "archived" | "pending" | "restricted";
  encrypted: boolean;
  hash: string;
  tags: string[];
  classification: "confidential" | "restricted" | "internal" | "public";
}

export interface Case {
  id: string;
  title: string;
  description: string;
  status: "open" | "under_investigation" | "closed" | "archived" | "pending_review";
  priority: "critical" | "high" | "medium" | "low";
  assignee: string;
  assigneeName: string;
  createdAt: string;
  updatedAt: string;
  documentsCount: number;
  evidenceCount: number;
  tags: string[];
  category: string;
}

export interface Evidence {
  id: string;
  title: string;
  type: "digital" | "physical" | "testimonial" | "documentary";
  caseId: string;
  caseTitle: string;
  collectedBy: string;
  collectedAt: string;
  status: "stored" | "in_transit" | "under_review" | "returned" | "destroyed";
  chainOfCustody: ChainEntry[];
  hash?: string;
  encrypted: boolean;
  description: string;
}

export interface ChainEntry {
  action: string;
  by: string;
  at: string;
  notes: string;
}

export interface AuditLog {
  id: string;
  userId: string;
  userName: string;
  action: string;
  resource: string;
  resourceId: string;
  details: string;
  timestamp: string;
  ipAddress: string;
  status: "success" | "failure" | "warning";
  category: "authentication" | "document" | "case" | "user" | "system" | "security";
}

export interface Report {
  id: string;
  title: string;
  type: "case_summary" | "audit_trail" | "security" | "activity" | "compliance";
  generatedBy: string;
  generatedAt: string;
  dateRange: { from: string; to: string };
  status: "completed" | "generating" | "failed";
}

export interface DashboardStats {
  totalDocuments: number;
  totalCases: number;
  activeUsers: number;
  pendingReviews: number;
  documentsThisWeek: number;
  casesThisMonth: number;
  securityAlerts: number;
  storageUsed: number;
}
