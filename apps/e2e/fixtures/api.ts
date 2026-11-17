import { APIRequestContext, request } from '@playwright/test';

export const API_URL = process.env.E2E_API_URL ?? 'https://api.manejoprime.com.br';
export const BASE_URL = process.env.E2E_BASE_URL ?? 'https://manejoprime.com.br';
export const SEED_SECRET = process.env.E2E_SEED_SECRET ?? '';
export const WA_WEBHOOK_SECRET = process.env.E2E_WHATSAPP_WEBHOOK_SECRET ?? '';
export const SKIP_AUDIO = process.env.E2E_SKIP_AUDIO === 'true';

export function authedHeaders(token: string) {
  return { Authorization: `Bearer ${token}` };
}

export async function createApiContext(token?: string): Promise<APIRequestContext> {
  return request.newContext({
    baseURL: API_URL,
    extraHTTPHeaders: token ? authedHeaders(token) : {},
  });
}

export async function assertOk(response: { ok(): boolean; status(): number; text(): Promise<string> }, label: string) {
  if (!response.ok()) {
    const body = await response.text();
    throw new Error(`${label} failed: HTTP ${response.status()} — ${body}`);
  }
}
