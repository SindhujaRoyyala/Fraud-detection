import type { User, Document, Case, Evidence, AuditLog, DashboardStats } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8081";
const AUTH_STORAGE_KEY = "dems_auth_session";

function getStoredAuth() {
  try {
    const raw = localStorage.getItem(AUTH_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function saveAuth(auth: { accessToken: string; refreshToken: string; user?: unknown } | null) {
  if (!auth) {
    localStorage.removeItem(AUTH_STORAGE_KEY);
    return;
  }

  localStorage.setItem(AUTH_STORAGE_KEY, JSON.stringify(auth));
}

function normalizeCollection<T>(payload: unknown): T[] {
  if (Array.isArray(payload)) return payload as T[];
  if (payload && typeof payload === "object") {
    const container = payload as { content?: unknown[]; items?: unknown[]; data?: unknown[] };
    if (Array.isArray(container.content)) return container.content as T[];
    if (Array.isArray(container.items)) return container.items as T[];
    if (Array.isArray(container.data)) return container.data as T[];
  }
  return [];
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const auth = getStoredAuth();
  const headers = new Headers(options.headers ?? {});

  if (!(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }

  if (auth?.accessToken) {
    headers.set("Authorization", `Bearer ${auth.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: "include",
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = payload?.message || payload?.error?.message || "Request failed";
    throw new Error(message);
  }

  return (payload?.data ?? payload ?? null) as T;
}

function mapStatus(status?: string): Case["status"] {
  const value = String(status ?? "").toLowerCase();
  if (["active", "open"].includes(value)) return "open";
  if (value === "under_investigation") return "under_investigation";
  if (value === "closed") return "closed";
  if (value === "archived") return "archived";
  if (value === "pending_review") return "pending_review";
  return "open";
}

function mapPriority(priority?: string): Case["priority"] {
  const value = String(priority ?? "").toLowerCase();
  if (value === "critical") return "critical";
  if (value === "high") return "high";
  if (value === "medium") return "medium";
  if (value === "low") return "low";
  return "medium";
}

function mapDocumentType(type?: string): Document["type"] {
  const value = String(type ?? "").toLowerCase();
  if (value.includes("pdf")) return "pdf";
  if (value.includes("doc")) return "docx";
  if (value.includes("xls")) return "xlsx";
  if (value.includes("image") || value.includes("png") || value.includes("jpg") || value.includes("jpeg")) return "image";
  if (value.includes("video")) return "video";
  if (value.includes("audio")) return "audio";
  return "other";
}

function mapEvidenceStatus(status?: string): Evidence["status"] {
  const value = String(status ?? "").toLowerCase();
  if (value.includes("transit")) return "in_transit";
  if (value.includes("review")) return "under_review";
  if (value.includes("return")) return "returned";
  if (value.includes("destroy")) return "destroyed";
  if (value.includes("stored") || value.includes("custody") || value.includes("collected")) return "stored";
  return "stored";
}

export const api = {
  auth: {
    async login(credentials: { username: string; password: string }) {
      const response = await request<{ tokens?: { accessToken?: string; refreshToken?: string }; user?: unknown }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(credentials),
      });

      const auth = {
        accessToken: response.tokens?.accessToken ?? "",
        refreshToken: response.tokens?.refreshToken ?? "",
        user: response.user ?? null,
      };

      saveAuth(auth);
      return response;
    },

    async register(payload: { username: string; email: string; password: string; fullName?: string; role?: string }) {
      const response = await request<{ tokens?: { accessToken?: string; refreshToken?: string }; user?: unknown }>("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({ ...payload, role: payload.role ?? "VIEWER" }),
      });

      const auth = {
        accessToken: response.tokens?.accessToken ?? "",
        refreshToken: response.tokens?.refreshToken ?? "",
        user: response.user ?? null,
      };

      saveAuth(auth);
      return response;
    },

    async me() {
      return request<{ id?: string; username?: string; email?: string; fullName?: string; role?: string }>("/api/auth/me");
    },

    async logout() {
      try {
        await request("/api/auth/logout", { method: "POST" });
      } finally {
        saveAuth(null);
      }
    },
  },

  dashboard: {
    async getStats(): Promise<DashboardStats> {
      const data = await request<Record<string, unknown>>("/api/dashboard/stats");
      return {
        totalDocuments: Number(data.totalDocuments ?? data.documentsProcessed ?? 0),
        totalCases: Number(data.totalCases ?? 0),
        activeUsers: Number(data.activeUsers ?? 0),
        pendingReviews: Number(data.documentsPendingProcessing ?? 0),
        documentsThisWeek: Number(data.documentsProcessed ?? 0),
        casesThisMonth: Number(data.activeCases ?? 0),
        securityAlerts: Number(data.totalAuditEvents ?? 0),
        storageUsed: 0,
      };
    },
  },

  documents: {
    async list(filters?: { search?: string; type?: string; status?: string }): Promise<Document[]> {
      const params = new URLSearchParams();
      if (filters?.search) params.set("search", filters.search);
      if (filters?.type) params.set("status", filters.type);
      if (filters?.status) params.set("status", filters.status);
      const data = await request<unknown>(`/api/documents${params.toString() ? `?${params.toString()}` : ""}`);
      return normalizeCollection<Record<string, unknown>>(data).map((item) => ({
        id: String(item.id ?? ""),
        title: String(item.title ?? item.originalFilename ?? "Untitled document"),
        type: mapDocumentType(String(item.mimeType ?? item.type ?? "")),
        category: String(item.category ?? "evidence") as Document["category"],
        caseId: item.caseId ? String(item.caseId) : undefined,
        uploadedBy: String(item.uploadedBy ?? item.uploadedByName ?? "System"),
        uploadedAt: String(item.createdAt ?? new Date().toISOString()),
        size: Number(item.fileSizeBytes ?? item.size ?? 0),
        status: String(item.status ?? "active") as Document["status"],
        encrypted: Boolean(item.encrypted ?? true),
        hash: String(item.hash ?? "sha256:unknown"),
        tags: Array.isArray(item.tags) ? item.tags.map((tag) => String(tag)) : [],
        classification: String(item.classification ?? "internal") as Document["classification"],
      }));
    },

    async getById(id: string): Promise<Document | undefined> {
      const data = await request<Record<string, unknown>>(`/api/documents/${id}`);
      if (!data) return undefined;
      return {
        id: String(data.id ?? id),
        title: String(data.title ?? "Untitled document"),
        type: mapDocumentType(String(data.mimeType ?? data.type ?? "")),
        category: String(data.category ?? "evidence") as Document["category"],
        caseId: data.caseId ? String(data.caseId) : undefined,
        uploadedBy: String(data.uploadedBy ?? data.uploadedByName ?? "System"),
        uploadedAt: String(data.createdAt ?? new Date().toISOString()),
        size: Number(data.fileSizeBytes ?? data.size ?? 0),
        status: String(data.status ?? "active") as Document["status"],
        encrypted: Boolean(data.encrypted ?? true),
        hash: String(data.hash ?? "sha256:unknown"),
        tags: Array.isArray(data.tags) ? data.tags.map((tag) => String(tag)) : [],
        classification: String(data.classification ?? "internal") as Document["classification"],
      };
    },
  },

  cases: {
    async list(filters?: { search?: string; status?: string; priority?: string }): Promise<Case[]> {
      const params = new URLSearchParams();
      if (filters?.search) params.set("search", filters.search);
      if (filters?.status) params.set("status", filters.status);
      if (filters?.priority) params.set("priority", filters.priority);
      const data = await request<unknown>(`/api/cases${params.toString() ? `?${params.toString()}` : ""}`);
      return normalizeCollection<Record<string, unknown>>(data).map((item) => ({
        id: String(item.id ?? ""),
        title: String(item.title ?? item.caseNumber ?? "Untitled case"),
        description: String(item.description ?? ""),
        status: mapStatus(String(item.status ?? "")),
        priority: mapPriority(String(item.priority ?? "")),
        assignee: String(item.assignedInvestigatorName ?? item.assignedLegalOfficerName ?? "Unassigned"),
        assigneeName: String(item.assignedInvestigatorName ?? item.assignedLegalOfficerName ?? "Unassigned"),
        createdAt: String(item.createdAt ?? new Date().toISOString()),
        updatedAt: String(item.updatedAt ?? item.createdAt ?? new Date().toISOString()),
        documentsCount: Number(item.documentsCount ?? 0),
        evidenceCount: Number(item.evidenceCount ?? 0),
        tags: Array.isArray(item.tags) ? item.tags.map((tag) => String(tag)) : [String(item.caseType ?? "general")],
        category: String(item.caseType ?? "general"),
      }));
    },

    async getById(id: string): Promise<Case | undefined> {
      const data = await request<Record<string, unknown>>(`/api/cases/${id}`);
      if (!data) return undefined;
      return {
        id: String(data.id ?? id),
        title: String(data.title ?? data.caseNumber ?? "Untitled case"),
        description: String(data.description ?? ""),
        status: mapStatus(String(data.status ?? "")),
        priority: mapPriority(String(data.priority ?? "")),
        assignee: String(data.assignedInvestigatorName ?? data.assignedLegalOfficerName ?? "Unassigned"),
        assigneeName: String(data.assignedInvestigatorName ?? data.assignedLegalOfficerName ?? "Unassigned"),
        createdAt: String(data.createdAt ?? new Date().toISOString()),
        updatedAt: String(data.updatedAt ?? data.createdAt ?? new Date().toISOString()),
        documentsCount: Number(data.documentsCount ?? 0),
        evidenceCount: Number(data.evidenceCount ?? 0),
        tags: Array.isArray(data.tags) ? data.tags.map((tag) => String(tag)) : [String(data.caseType ?? "general")],
        category: String(data.caseType ?? "general"),
      };
    },
  },

  evidence: {
    async list(filters?: { search?: string; status?: string; caseId?: string }): Promise<Evidence[]> {
      const params = new URLSearchParams();
      if (filters?.search) params.set("search", filters.search);
      if (filters?.status) params.set("status", filters.status);
      if (filters?.caseId) params.set("caseId", filters.caseId);
      const data = await request<unknown>(`/api/evidence${params.toString() ? `?${params.toString()}` : ""}`);
      return normalizeCollection<Record<string, unknown>>(data).map((item) => ({
        id: String(item.id ?? ""),
        title: String(item.title ?? "Unnamed evidence"),
        type: String(item.evidenceType ?? item.type ?? "digital") as Evidence["type"],
        caseId: String(item.caseId ?? ""),
        caseTitle: String(item.caseTitle ?? item.caseName ?? "Unknown case"),
        collectedBy: String(item.currentCustodianName ?? item.collectedBy ?? "System"),
        collectedAt: String(item.createdAt ?? item.collectedAt ?? new Date().toISOString()),
        status: mapEvidenceStatus(String(item.status ?? "")),
        chainOfCustody: Array.isArray(item.chainOfCustody) ? item.chainOfCustody.map((entry) => ({
          action: String((entry as Record<string, unknown>).action ?? "Updated"),
          by: String((entry as Record<string, unknown>).by ?? "System"),
          at: String((entry as Record<string, unknown>).at ?? new Date().toISOString()),
          notes: String((entry as Record<string, unknown>).notes ?? ""),
        })) : [],
        hash: item.hash ? String(item.hash) : undefined,
        encrypted: Boolean(item.encrypted ?? true),
        description: String(item.description ?? ""),
      }));
    },
  },

  users: {
    async list(filters?: { search?: string; role?: string; status?: string }): Promise<User[]> {
      const params = new URLSearchParams();
      if (filters?.search) params.set("search", filters.search);
      if (filters?.role) params.set("role", filters.role.toUpperCase());
      if (filters?.status) params.set("status", filters.status);
      const data = await request<unknown>(`/api/users${params.toString() ? `?${params.toString()}` : ""}`);
      return normalizeCollection<Record<string, unknown>>(data).map((item) => ({
        id: String(item.id ?? ""),
        name: String(item.fullName ?? item.username ?? "Unknown user"),
        email: String(item.email ?? ""),
        role: String(item.role ?? "viewer").toLowerCase() as User["role"],
        department: "Operations",
        status: Boolean(item.active) ? "active" : "inactive",
        createdAt: String(item.createdAt ?? new Date().toISOString()),
        lastLogin: item.lastLoginAt ? String(item.lastLoginAt) : "-",
        twoFactorEnabled: false,
      }));
    },
  },

  audit: {
    async list(filters?: { search?: string; category?: string; status?: string }): Promise<AuditLog[]> {
      const params = new URLSearchParams();
      if (filters?.search) params.set("search", filters.search);
      if (filters?.category) params.set("event", filters.category.toUpperCase());
      if (filters?.status) params.set("status", filters.status);
      const data = await request<unknown>(`/api/audit/logs${params.toString() ? `?${params.toString()}` : ""}`);
      return normalizeCollection<Record<string, unknown>>(data).map((item) => ({
        id: String(item.id ?? ""),
        userId: String(item.userId ?? ""),
        userName: String(item.username ?? "System"),
        action: String(item.event ?? "Activity"),
        resource: String(item.resource ?? "system"),
        resourceId: String(item.resourceId ?? ""),
        details: String(item.description ?? ""),
        timestamp: String(item.createdAt ?? new Date().toISOString()),
        ipAddress: String(item.ipAddress ?? "N/A"),
        status: String(item.status ?? "success") as AuditLog["status"],
        category: String(item.category ?? "system") as AuditLog["category"],
      }));
    },
  },
};

export function getStoredAuthSession() {
  return getStoredAuth();
}

export function clearStoredAuthSession() {
  saveAuth(null);
}
