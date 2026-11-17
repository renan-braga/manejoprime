import { request } from '@playwright/test';
import { API_URL, SEED_SECRET } from './api';

export interface SeedContext {
  accessToken: string;
  refreshToken: string;
  farmId: string;
  userId: string;
  phone: string;
}

export async function seedFarm(): Promise<SeedContext> {
  const ctx = await request.newContext({ baseURL: API_URL });
  const res = await ctx.post('/api/v1/testing/seed', {
    headers: { 'X-Test-Seed-Secret': SEED_SECRET },
  });

  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`Seed failed: HTTP ${res.status()} — ${body}`);
  }

  return res.json() as Promise<SeedContext>;
}

export async function teardownFarm(farmId: string): Promise<void> {
  const ctx = await request.newContext({ baseURL: API_URL });
  const res = await ctx.delete(`/api/v1/testing/seed/${farmId}`, {
    headers: { 'X-Test-Seed-Secret': SEED_SECRET },
  });

  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`Teardown failed: HTTP ${res.status()} — ${body}`);
  }
}
