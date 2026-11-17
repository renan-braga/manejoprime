import { request } from '@playwright/test';
import { writeFileSync } from 'fs';
import { join } from 'path';
import { API_URL, SEED_SECRET } from './fixtures/api';

export const CONTEXT_FILE = join(__dirname, '.e2e-context.json');

async function globalSetup() {
  const ctx = await request.newContext({ baseURL: API_URL });
  const res = await ctx.post('/api/v1/testing/seed', {
    headers: { 'X-Test-Seed-Secret': SEED_SECRET },
  });

  if (!res.ok()) {
    const body = await res.text();
    throw new Error(`[global-setup] Seed failed: HTTP ${res.status()} — ${body}`);
  }

  const data = await res.json();
  writeFileSync(CONTEXT_FILE, JSON.stringify(data));
  console.log(`\n[global-setup] Farm seeded: ${data.farmId} (phone: ${data.phone})\n`);
}

export default globalSetup;
