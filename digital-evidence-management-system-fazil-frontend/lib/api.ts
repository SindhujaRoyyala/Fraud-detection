const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined) || 'http://localhost:8083';
const STORAGE_KEY = 'dems_auth_session';

export type StoredAuthSession = {
  accessToken: string;
  refreshToken: string;
  user?: Record<string, any>;
  expiresIn?: number;
};

function safeReadSession(): StoredAuthSession | null {
  if (typeof window === 'undefined') return null;

  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

function safeWriteSession(data: StoredAuthSession) {
  if (typeof window === 'undefined') return;
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}

export function clearStoredAuthSession() {
  if (typeof window !== 'undefined') {
    window.localStorage.removeItem(STORAGE_KEY);
  }
}

export function getStoredAuthSession() {
  return safeReadSession();
}

async function apiFetch<T>(path: string, options: RequestInit = {}, requireAuth = true): Promise<T> {
  const session = safeReadSession();
  const headers = new Headers(options.headers || {});

  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  if (requireAuth && session?.accessToken) {
    headers.set('Authorization', `Bearer ${session.accessToken}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    credentials: 'include',
  });

  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;

  if (!response.ok) {
    const message = payload?.error?.message || payload?.message || `Request failed with status ${response.status}`;
    throw new Error(message);
  }

  return payload?.data ?? payload ?? (null as T);
}

export const api = {
  auth: {
    async login(payload: { username: string; password: string }) {
      const response = await apiFetch<{ tokens: { accessToken: string; refreshToken: string; expiresIn?: number }; user: Record<string, any> }>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify(payload),
      }, false);

      const session: StoredAuthSession = {
        accessToken: response.tokens.accessToken,
        refreshToken: response.tokens.refreshToken,
        expiresIn: response.tokens.expiresIn,
        user: response.user,
      };
      safeWriteSession(session);
      return response;
    },
    async register(payload: { username: string; email: string; password: string; fullName: string; role?: string }) {
      const response = await apiFetch<{ tokens: { accessToken: string; refreshToken: string; expiresIn?: number }; user: Record<string, any> }>('/api/auth/register', {
        method: 'POST',
        body: JSON.stringify(payload),
      }, false);

      const session: StoredAuthSession = {
        accessToken: response.tokens.accessToken,
        refreshToken: response.tokens.refreshToken,
        expiresIn: response.tokens.expiresIn,
        user: response.user,
      };
      safeWriteSession(session);
      return response;
    },
    async me() {
      return await apiFetch<Record<string, any>>('/api/auth/me');
    },
    async logout() {
      try {
        await apiFetch('/api/auth/logout', { method: 'POST' });
      } finally {
        clearStoredAuthSession();
      }
    },
  },
  dashboard: {
    async getStats() {
      return await apiFetch('/api/dashboard/stats');
    },
  },
  cases: {
    async list() {
      return await apiFetch<any>('/api/cases');
    },
    async get(id: string) {
      return await apiFetch(`/api/cases/${id}`);
    },
    async create(payload: Record<string, any>) {
      return await apiFetch('/api/cases', { method: 'POST', body: JSON.stringify(payload) });
    },
    async update(id: string, payload: Record<string, any>) {
      return await apiFetch(`/api/cases/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
    },
  },
  evidence: {
    async list() {
      return await apiFetch<any>('/api/evidence');
    },
    async get(id: string) {
      return await apiFetch(`/api/evidence/${id}`);
    },
    async create(payload: Record<string, any>) {
      return await apiFetch('/api/evidence', { method: 'POST', body: JSON.stringify(payload) });
    },
  },
  audit: {
    async list() {
      return await apiFetch<any>('/api/audit/logs');
    },
  },
  users: {
    async list() {
      return await apiFetch<any>('/api/users');
    },
  },
};
