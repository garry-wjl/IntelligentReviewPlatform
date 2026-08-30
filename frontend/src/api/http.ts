export type ApiEnvelope<T> = {
  code: number;
  msg?: string;
  message?: string;
  data?: T;
  rows?: T;
};

export class ApiError extends Error {
  code?: number;

  constructor(message: string, code?: number) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
  }
}

function baseURL() {
  return import.meta.env.VITE_API_BASE || '/admin/v1';
}

function authHeader() {
  const token = localStorage.getItem('token') || 'dev-sso';
  return `Bearer ${token}`;
}

function joinURL(path: string) {
  if (/^https?:\/\//i.test(path)) return path;
  const base = baseURL().replace(/\/$/, '');
  const suffix = path.startsWith('/') ? path : `/${path}`;
  return `${base}${suffix}`;
}

function unwrap<T>(payload: ApiEnvelope<T>): T {
  if (payload.code !== 200) {
    throw new ApiError(payload.msg || payload.message || '请求失败', payload.code);
  }
  if (payload.data !== undefined && payload.data !== null) return payload.data;
  if (payload.rows !== undefined && payload.rows !== null) return payload.rows;
  return undefined as T;
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set('Authorization', authHeader());
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(joinURL(path), { ...init, headers });
  let payload: ApiEnvelope<T>;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    throw new ApiError(response.ok ? '响应不是合法 JSON' : `HTTP ${response.status}`, response.status);
  }
  return unwrap(payload);
}

export function get<T>(path: string, query?: Record<string, string | number | boolean | undefined | null>) {
  const params = new URLSearchParams();
  if (query) {
    Object.entries(query).forEach(([key, value]) => {
      if (value === undefined || value === null || value === '') return;
      params.set(key, String(value));
    });
  }
  const search = params.toString();
  return request<T>(search ? `${path}?${search}` : path);
}

export function post<T>(path: string, body?: unknown) {
  return request<T>(path, {
    method: 'POST',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}
