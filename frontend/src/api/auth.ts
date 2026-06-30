import { api, setAccessToken } from './http';
import type { ApiResponse, AuthResponse, CurrentUser } from './types';

export async function register(email: string, password: string): Promise<AuthResponse> {
  const res = await api.post<ApiResponse<AuthResponse>>(
    '/api/v1/auth/register',
    { email, password, acceptedTerms: true },
    { auth: false },
  );
  setAccessToken(res.data.accessToken);
  return res.data;
}

export async function login(email: string, password: string): Promise<AuthResponse> {
  const res = await api.post<ApiResponse<AuthResponse>>(
    '/api/v1/auth/login',
    { email, password },
    { auth: false },
  );
  setAccessToken(res.data.accessToken);
  return res.data;
}

/** Tenta restaurar a sessão no boot usando o cookie de refresh. */
export async function bootstrapSession(): Promise<CurrentUser | null> {
  try {
    const refreshed = await api.post<ApiResponse<AuthResponse>>(
      '/api/v1/auth/refresh',
      undefined,
      { auth: false, retryOnUnauthorized: false },
    );
    setAccessToken(refreshed.data.accessToken);
    return { userId: refreshed.data.userId, email: refreshed.data.email };
  } catch {
    setAccessToken(null);
    return null;
  }
}

export async function logout(): Promise<void> {
  try {
    await api.post<void>('/api/v1/auth/logout');
  } finally {
    setAccessToken(null);
  }
}

export function me(): Promise<ApiResponse<CurrentUser>> {
  return api.get<ApiResponse<CurrentUser>>('/api/v1/auth/me');
}
