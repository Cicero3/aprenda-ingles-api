import type { ApiErrorBody, ApiResponse } from './types';

// Base da API. Em dev fica vazio -> usa o proxy /api do Vite (same-origin, cookie funciona).
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '';

// Access token (curto) mantido SÓ em memória — nunca em localStorage (XSS).
// O refresh token vive em cookie httpOnly e é tratado pelo navegador.
let accessToken: string | null = null;
export const setAccessToken = (token: string | null) => { accessToken = token; };
export const getAccessToken = () => accessToken;

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  auth?: boolean; // anexa o Authorization header (default true)
  retryOnUnauthorized?: boolean; // tenta refresh + repete uma vez (default true)
}

async function raw(path: string, opts: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' };
  if (opts.auth !== false && accessToken) headers.Authorization = `Bearer ${accessToken}`;
  return fetch(`${BASE_URL}${path}`, {
    method: opts.method ?? 'GET',
    headers,
    credentials: 'include', // envia/recebe o cookie httpOnly do refresh token
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined,
  });
}

/** Tenta renovar o access token usando o cookie de refresh. Retorna true se renovou. */
async function tryRefresh(): Promise<boolean> {
  const res = await raw('/api/v1/auth/refresh', { method: 'POST', auth: false });
  if (!res.ok) return false;
  const body = (await res.json()) as ApiResponse<{ accessToken: string }>;
  setAccessToken(body.data.accessToken);
  return true;
}

async function parse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;
  const text = await res.text();
  const json = text ? JSON.parse(text) : undefined;
  if (!res.ok) {
    const err = (json as ApiErrorBody | undefined)?.error;
    throw new ApiError(res.status, err?.code ?? 'UNKNOWN', err?.message ?? res.statusText);
  }
  return json as T;
}

export async function request<T>(path: string, opts: RequestOptions = {}): Promise<T> {
  let res = await raw(path, opts);

  if (res.status === 401 && opts.retryOnUnauthorized !== false && opts.auth !== false) {
    const refreshed = await tryRefresh();
    if (refreshed) {
      res = await raw(path, opts); // repete uma vez com o novo token
    } else {
      setAccessToken(null);
    }
  }

  return parse<T>(res);
}

export const api = {
  get: <T>(path: string, opts?: RequestOptions) => request<T>(path, { ...opts, method: 'GET' }),
  post: <T>(path: string, body?: unknown, opts?: RequestOptions) =>
    request<T>(path, { ...opts, method: 'POST', body }),
};
