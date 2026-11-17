import { readFileSync } from 'fs';
import { join } from 'path';

interface E2EContext {
  accessToken: string;
  refreshToken: string;
  farmId: string;
  userId: string;
  phone: string;
}

let _ctx: E2EContext | null = null;

function ctx(): E2EContext {
  if (!_ctx) {
    const file = join(__dirname, '../.e2e-context.json');
    try {
      _ctx = JSON.parse(readFileSync(file, 'utf-8'));
    } catch {
      throw new Error(
        '[e2e] .e2e-context.json not found — global setup may have failed'
      );
    }
  }
  return _ctx!;
}

export function token(): string { return ctx().accessToken; }
export function refreshToken(): string { return ctx().refreshToken; }
export function farmId(): string { return ctx().farmId; }
export function userId(): string { return ctx().userId; }
export function phone(): string { return ctx().phone; }
