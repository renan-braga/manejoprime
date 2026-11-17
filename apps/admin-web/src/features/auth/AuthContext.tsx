import { createContext, ReactNode, useCallback, useContext, useEffect, useMemo, useRef, useState } from "react";
import { readApiBaseUrl } from "../../config/runtimeConfig";
import type { OAuthProvider } from "./oauthPkce";

export type AuthStatus = "loading" | "signed-out" | "authenticated";

export type SessionMe = {
  userId: string;
  name: string;
  email?: string | null;
  phone?: string | null;
  role: string;
  farmId: string;
  farmName: string;
  permissions: string[];
  billing?: BillingContext;
  farmCount?: number;
  animalCount?: number;
  userCount?: number;
};

export type BillingContext = {
  plan: string;
  status: string;
  currentPeriodEnd?: string | null;
  graceUntil?: string | null;
  billingExempt: boolean;
  exemptionReason?: string | null;
  canWrite: boolean;
  canUseWhatsapp: boolean;
};

type AuthTokens = {
  accessToken: string;
  refreshToken: string;
  accessTokenExpiresAt?: string;
  refreshTokenExpiresAt?: string;
};

type OAuthCallbackApiResponse = {
  status: "authenticated" | "onboarding_required";
  accessToken?: string | null;
  refreshToken?: string | null;
  tokenType?: string | null;
  accessTokenExpiresAt?: string | null;
  refreshTokenExpiresAt?: string | null;
  onboardingToken?: string | null;
};

export type LoginWithOAuthResult =
  | { status: "authenticated" }
  | { status: "onboarding_required"; onboardingToken: string };

type AuthContextValue = {
  status: AuthStatus;
  session: SessionMe | null;
  authFailureReason: string | null;
  loginWithOAuth: (provider: OAuthProvider, code: string, codeVerifier: string, redirectUri: string) => Promise<LoginWithOAuthResult>;
  registerFarm: (onboardingToken: string, farmName: string) => Promise<void>;
  switchFarm: (farmId: string) => Promise<void>;
  fetchProtectedJson: <TResponse>(path: string, init?: RequestInit) => Promise<TResponse>;
  refreshBillingStatus: () => Promise<BillingContext | null>;
  logout: () => Promise<void>;
  clearAuthFailureReason: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

const STORAGE_KEY = "curralzap.admin.auth";
const API_BASE_URL = readApiBaseUrl();
const ACCESS_TOKEN_REFRESH_SKEW_MS = 2 * 60 * 1000;

let refreshInFlight: Promise<AuthTokens> | null = null;

type AuthProviderProps = {
  children?: ReactNode;
  initialStatus?: AuthStatus;
  initialSession?: SessionMe | null;
  initialTokens?: AuthTokens | null;
};

export function AuthProvider({
  children,
  initialStatus = "signed-out",
  initialSession = null,
  initialTokens = null
}: AuthProviderProps) {
  const [status, setStatus] = useState<AuthStatus>(initialStatus === "authenticated" ? "authenticated" : "loading");
  const [session, setSession] = useState<SessionMe | null>(initialSession);
  const [tokens, setTokens] = useState<AuthTokens | null>(initialTokens);
  const [authFailureReason, setAuthFailureReason] = useState<string | null>(null);
  const tokensRef = useRef(tokens);
  useEffect(() => { tokensRef.current = tokens; }, [tokens]);

  useEffect(() => {
    if (initialStatus === "authenticated") {
      setSession(initialSession);
      setTokens(initialTokens);
      setStatus("authenticated");
      return;
    }

    void restoreSession();
  }, [initialSession, initialStatus, initialTokens]);

  async function restoreSession() {
    let storedTokens = readStoredTokens();
    if (storedTokens == null) {
      setTokens(null);
      setStatus("signed-out");
      return;
    }

    try {
      if (shouldRefreshAccessToken(storedTokens)) {
        storedTokens = await refreshAuthTokens(storedTokens.refreshToken);
        persistTokens(storedTokens);
      }
      const restoredSession = await fetchSession(storedTokens.accessToken);
      setTokens(storedTokens);
      setSession(restoredSession);
      setStatus("authenticated");
    } catch (error) {
      if (isApiErrorCode(error, "token_expired") || isApiErrorCode(error, "authentication_failed")) {
        try {
          const refreshedTokens = await refreshAuthTokens(storedTokens.refreshToken);
          persistTokens(refreshedTokens);
          const restoredSession = await fetchSession(refreshedTokens.accessToken);
          setTokens(refreshedTokens);
          setSession(restoredSession);
          setStatus("authenticated");
          return;
        } catch (refreshError) {
          forceSignOut(refreshError);
          return;
        }
      } else {
        forceSignOut(error);
        return;
      }
    }
  }

  async function loginWithOAuth(provider: OAuthProvider, code: string, codeVerifier: string, redirectUri: string): Promise<LoginWithOAuthResult> {
    const response = await postJson<OAuthCallbackApiResponse>("/api/v1/auth/oauth/callback", { provider, code, codeVerifier, redirectUri });
    if (response.status === "onboarding_required") {
      return { status: "onboarding_required", onboardingToken: response.onboardingToken! };
    }
    const tokenPair: AuthTokens = {
      accessToken: response.accessToken!,
      refreshToken: response.refreshToken!,
      accessTokenExpiresAt: response.accessTokenExpiresAt ?? undefined,
      refreshTokenExpiresAt: response.refreshTokenExpiresAt ?? undefined
    };
    persistTokens(tokenPair);
    setTokens(tokenPair);
    const nextSession = await fetchSession(tokenPair.accessToken);
    setSession(nextSession);
    setStatus("authenticated");
    setAuthFailureReason(null);
    return { status: "authenticated" };
  }

  async function registerFarm(onboardingToken: string, farmName: string) {
    const tokenPair = await postJson<AuthTokens>("/api/v1/onboarding/register", { onboardingToken, farmName });
    persistTokens(tokenPair);
    setTokens(tokenPair);
    const nextSession = await fetchSession(tokenPair.accessToken);
    setSession(nextSession);
    setStatus("authenticated");
    setAuthFailureReason(null);
    localStorage.setItem("curralzap.admin.tour_pending", "true");
  }

  const forceSignOut = useCallback((error: unknown) => {
    clearStoredTokens();
    setTokens(null);
    setSession(null);
    setStatus("signed-out");
    setAuthFailureReason(describeForcedReauthReason(error));
  }, []);

  const logout = useCallback(async () => {
    const activeTokens = tokensRef.current ?? readStoredTokens();
    if (activeTokens != null) {
      try {
        await postJson("/api/v1/auth/logout", { refreshToken: activeTokens.refreshToken });
      } catch {
        // Logout should still clear local state even if the backend session is already gone.
      }
    }

    clearStoredTokens();
    setTokens(null);
    setSession(null);
    setStatus("signed-out");
    setAuthFailureReason(null);
  }, []);

  const fetchProtectedJson: AuthContextValue["fetchProtectedJson"] = useCallback(
    async (path: string, init?: RequestInit) => {
      let activeTokens = tokensRef.current ?? readStoredTokens();
      if (activeTokens == null) {
        const error = {
          code: "authentication_required",
          message: "Sign in again to continue."
        };
        forceSignOut(error);
        throw error;
      }

      try {
        if (shouldRefreshAccessToken(activeTokens)) {
          activeTokens = await refreshAuthTokens(activeTokens.refreshToken);
          persistTokens(activeTokens);
          setTokens(activeTokens);
        }
        return await fetchWithAccessToken(path, activeTokens.accessToken, init);
      } catch (error) {
        if (!isApiErrorCode(error, "token_expired") && !isApiErrorCode(error, "authentication_failed")) {
          throw error;
        }

        try {
          const refreshedTokens = await refreshAuthTokens(activeTokens.refreshToken);
          persistTokens(refreshedTokens);
          setTokens(refreshedTokens);
          return await fetchWithAccessToken(path, refreshedTokens.accessToken, init);
        } catch (refreshError) {
          forceSignOut(refreshError);
          throw refreshError;
        }
      }
    },
    [forceSignOut]
  );

  const switchFarm = useCallback(async (farmId: string) => {
    const tokenPair = await fetchProtectedJson<AuthTokens>(`/api/v1/auth/switch-farm/${farmId}`, {
      method: "POST"
    });
    persistTokens(tokenPair);
    setTokens(tokenPair);
    const nextSession = await fetchSession(tokenPair.accessToken);
    setSession(nextSession);
  }, [fetchProtectedJson]);

  const refreshBillingStatus = useCallback(async (): Promise<BillingContext | null> => {
    try {
      const newSession = await fetchProtectedJson<SessionMe>("/api/v1/session/me");
      setSession(newSession);
      return newSession.billing ?? null;
    } catch {
      return null;
    }
  }, [fetchProtectedJson]);

  const clearAuthFailureReason = useCallback(() => {
    setAuthFailureReason(null);
  }, []);

  const value = useMemo(
    () => ({
      status,
      session,
      authFailureReason,
      loginWithOAuth,
      registerFarm,
      switchFarm,
      fetchProtectedJson,
      refreshBillingStatus,
      logout,
      clearAuthFailureReason
    }),
    [authFailureReason, clearAuthFailureReason, fetchProtectedJson, logout, refreshBillingStatus, session, status, switchFarm]
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (value == null) {
    throw new Error("useAuth must be used within AuthProvider.");
  }
  return value;
}

function persistTokens(tokens: AuthTokens) {
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(tokens));
}

function readStoredTokens(): AuthTokens | null {
  const stored = window.localStorage.getItem(STORAGE_KEY);
  if (stored == null) {
    return null;
  }

  try {
    const parsed = JSON.parse(stored) as Partial<AuthTokens>;
    if (typeof parsed.accessToken !== "string" || typeof parsed.refreshToken !== "string") {
      return null;
    }
    return {
      accessToken: parsed.accessToken,
      refreshToken: parsed.refreshToken,
      accessTokenExpiresAt: typeof parsed.accessTokenExpiresAt === "string" ? parsed.accessTokenExpiresAt : undefined,
      refreshTokenExpiresAt: typeof parsed.refreshTokenExpiresAt === "string" ? parsed.refreshTokenExpiresAt : undefined
    };
  } catch {
    return null;
  }
}

function clearStoredTokens() {
  window.localStorage.removeItem(STORAGE_KEY);
}

async function fetchSession(accessToken: string) {
  return fetchJson<SessionMe>("/api/v1/session/me", {
    headers: {
      Authorization: `Bearer ${accessToken}`
    }
  });
}

async function fetchWithAccessToken<TResponse>(path: string, accessToken: string, init?: RequestInit) {
  return fetchJson<TResponse>(path, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      Authorization: `Bearer ${accessToken}`
    }
  });
}

async function refreshTokens(refreshToken: string) {
  return postJson<AuthTokens>("/api/v1/auth/refresh", { refreshToken });
}

async function refreshAuthTokens(refreshToken: string) {
  if (refreshInFlight != null) {
    return refreshInFlight;
  }

  refreshInFlight = refreshTokens(refreshToken).finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

function shouldRefreshAccessToken(tokens: AuthTokens) {
  const expiresAt = readAccessTokenExpiresAt(tokens);
  if (expiresAt == null) return false;
  return expiresAt.getTime() - Date.now() <= ACCESS_TOKEN_REFRESH_SKEW_MS;
}

function readAccessTokenExpiresAt(tokens: AuthTokens) {
  const apiExpiresAt = parseDate(tokens.accessTokenExpiresAt);
  if (apiExpiresAt != null) return apiExpiresAt;
  return parseJwtExpiresAt(tokens.accessToken);
}

function parseDate(value: string | undefined) {
  if (value == null || value.length === 0) return null;
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function parseJwtExpiresAt(token: string) {
  const payload = token.split(".")[1];
  if (payload == null) return null;

  try {
    const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
    const paddedPayload = normalizedPayload.padEnd(normalizedPayload.length + ((4 - normalizedPayload.length % 4) % 4), "=");
    const decodedPayload = JSON.parse(atob(paddedPayload)) as { exp?: unknown };
    return typeof decodedPayload.exp === "number" ? new Date(decodedPayload.exp * 1000) : null;
  } catch {
    return null;
  }
}

async function postJson<TResponse = void>(path: string, body: unknown) {
  return fetchJson<TResponse>(path, {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(body)
  });
}

async function fetchJson<TResponse>(path: string, init?: RequestInit) {
  const response = await fetch(`${API_BASE_URL}${path}`, init);

  if (!response.ok) {
    throw await parseApiError(response);
  }

  if (response.status === 204) {
    return undefined as TResponse;
  }

  return (await response.json()) as TResponse;
}

async function parseApiError(response: Response) {
  try {
    return (await response.json()) as { code?: string; message?: string };
  } catch {
    return {
      code: "unknown_error",
      message: "Unexpected response from the API."
    };
  }
}

function isApiErrorCode(error: unknown, code: string) {
  return typeof error === "object" && error != null && "code" in error && error.code === code;
}

function describeForcedReauthReason(error: unknown) {
  if (typeof error === "object" && error != null && "code" in error) {
    switch (error.code) {
      case "session_replaced":
        return "Your admin session was replaced by a newer login on another device. Sign in again to continue.";
      case "revoked_refresh_token":
      case "expired_refresh_token":
      case "invalid_refresh_token":
      case "token_expired":
      case "authentication_failed":
        return "Your previous admin session is no longer valid. Sign in again to continue.";
      default:
        break;
    }
  }

  return "We could not restore your previous admin session. Sign in again to continue.";
}
