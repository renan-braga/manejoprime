import { readApiBaseUrl } from "../../config/runtimeConfig";

const SESSION_KEY = "ops_api_key";

export function getStoredOpsKey(): string {
  return sessionStorage.getItem(SESSION_KEY) ?? "";
}

export function setStoredOpsKey(key: string): void {
  sessionStorage.setItem(SESSION_KEY, key);
}

export function clearStoredOpsKey(): void {
  sessionStorage.removeItem(SESSION_KEY);
}

async function opsFetch<T>(path: string): Promise<T> {
  const key = getStoredOpsKey();
  const res = await fetch(`${readApiBaseUrl()}${path}`, {
    headers: { "X-Ops-Api-Key": key },
  });
  if (res.status === 401) {
    clearStoredOpsKey();
    throw new OpsAuthError();
  }
  if (!res.ok) {
    throw new Error(`Ops request failed: ${res.status}`);
  }
  return res.json() as Promise<T>;
}

export class OpsAuthError extends Error {
  constructor() {
    super("ops_authentication_required");
  }
}

export type OpsSummary = {
  totalFarms: number;
  totalUsers: number;
  adminCount: number;
  vetCount: number;
  workerCount: number;
  totalAnimals: number;
  messagesLast24h: number;
  messagesLast7d: number;
  messagesLast30d: number;
  audioCount: number;
  transcriptionCount: number;
  errorCount: number;
};

export type OpsFarmListItem = {
  farmId: string;
  farmName: string;
  createdAt: string;
  userCount: number;
  animalCount: number;
  lastWhatsappActivity: string | null;
  messagesLast7d: number;
  errorsLast7d: number;
};

export type OpsUserActivity = {
  userId: string;
  name: string;
  role: string;
  active: boolean;
  lastActivity: string | null;
};

export type OpsRecentError = {
  messageId: string;
  farmId: string;
  userId: string | null;
  failureReason: string | null;
  receivedAt: string;
};

export type OpsFarmDetail = {
  farmId: string;
  farmName: string;
  createdAt: string;
  users: OpsUserActivity[];
  messagesLast30d: number;
  audioLast30d: number;
  transcribedLast30d: number;
  failedLast30d: number;
  recentErrors: OpsRecentError[];
};

export type OpsMessageStats = {
  period: string;
  totalMessages: number;
  audioMessages: number;
  textMessages: number;
  transcribed: number;
  applied: number;
  failed: number;
};

export type OpsMessageErrorPage = {
  errors: OpsRecentError[];
  total: number;
  limit: number;
  offset: number;
};

export const opsApi = {
  summary: () => opsFetch<OpsSummary>("/api/v1/ops/summary"),
  farms: () => opsFetch<OpsFarmListItem[]>("/api/v1/ops/farms"),
  farmDetail: (farmId: string) => opsFetch<OpsFarmDetail>(`/api/v1/ops/farms/${farmId}`),
  messageStats: (period: "24h" | "7d" | "30d" = "7d") =>
    opsFetch<OpsMessageStats>(`/api/v1/ops/messages/stats?period=${period}`),
  messageErrors: (limit = 50, offset = 0) =>
    opsFetch<OpsMessageErrorPage>(`/api/v1/ops/messages/errors?limit=${limit}&offset=${offset}`),
};
