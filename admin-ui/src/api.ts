// Tiny fetch wrapper for the jprime-admin API. The session is an HTTP-only cookie set by POST /j_security_check.

export class ApiError extends Error {
  constructor(
    public status: number,
    message: string,
    public fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

let onUnauthorized: () => void = () => {};

export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler;
}

async function request<T>(method: string, url: string, body?: unknown): Promise<T> {
  const init: RequestInit = { method, credentials: 'same-origin', headers: { Accept: 'application/json' } };
  if (body instanceof FormData) {
    init.body = body;
  } else if (body !== undefined) {
    init.body = JSON.stringify(body);
    (init.headers as Record<string, string>)['Content-Type'] = 'application/json';
  }
  const response = await fetch(url, init);
  if (response.status === 401) {
    onUnauthorized();
    throw new ApiError(401, 'Please log in');
  }
  if (!response.ok) {
    let message = `${response.status} ${response.statusText}`;
    let fieldErrors: Record<string, string> = {};
    try {
      const json = await response.json();
      message = json.message ?? message;
      fieldErrors = json.errors ?? {};
      if (Object.keys(fieldErrors).length > 0) {
        message = Object.values(fieldErrors).join(' ');
      }
    } catch {
      // not JSON
    }
    throw new ApiError(response.status, message, fieldErrors);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const type = response.headers.get('Content-Type') ?? '';
  return (type.includes('json') ? response.json() : response.text()) as Promise<T>;
}

export const api = {
  get: <T>(url: string) => request<T>('GET', url),
  post: <T>(url: string, body?: unknown) => request<T>('POST', url, body ?? {}),
  put: <T>(url: string, body: unknown) => request<T>('PUT', url, body),
  del: (url: string) => request<void>('DELETE', url),

  async login(username: string, password: string): Promise<boolean> {
    const form = new URLSearchParams({ j_username: username, j_password: password });
    const response = await fetch('/j_security_check', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: form,
    });
    return response.ok;
  },

  logout: () => request<void>('POST', '/api/auth/logout'),

  /** Downloads a binary response (CSV, PDF) and lets the browser save or open it. */
  async download(url: string, fileName: string, method = 'GET', body?: unknown) {
    const response = await fetch(url, {
      method,
      credentials: 'same-origin',
      headers: body ? { 'Content-Type': 'application/json' } : undefined,
      body: body ? JSON.stringify(body) : undefined,
    });
    if (!response.ok) {
      throw new ApiError(response.status, `${response.status} ${response.statusText}`);
    }
    const blob = await response.blob();
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = fileName;
    link.click();
    setTimeout(() => URL.revokeObjectURL(link.href), 10_000);
  },
};

/** Builds a query string, skipping empty values. */
export function query(params: Record<string, string | number | boolean | undefined | null>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  });
  const text = search.toString();
  return text ? `?${text}` : '';
}
