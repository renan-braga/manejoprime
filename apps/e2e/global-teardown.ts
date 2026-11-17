import { request } from '@playwright/test';
import { readFileSync, existsSync, unlinkSync } from 'fs';
import { join } from 'path';
import { API_URL, SEED_SECRET } from './fixtures/api';

const CONTEXT_FILE = join(__dirname, '.e2e-context.json');

async function globalTeardown() {
  if (!existsSync(CONTEXT_FILE)) return;

  const data = JSON.parse(readFileSync(CONTEXT_FILE, 'utf-8'));
  const ctx = await request.newContext({ baseURL: API_URL });
  const res = await ctx.delete(`/api/v1/testing/seed/${data.farmId}`, {
    headers: { 'X-Test-Seed-Secret': SEED_SECRET },
  });

  if (!res.ok()) {
    const body = await res.text();
    console.error(`[global-teardown] Teardown failed: HTTP ${res.status()} — ${body}`);
  } else {
    console.log(`\n[global-teardown] Farm ${data.farmId} cleaned up\n`);
    unlinkSync(CONTEXT_FILE);
  }
}

export default globalTeardown;
