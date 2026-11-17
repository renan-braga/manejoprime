export type OAuthProvider = "google" | "facebook" | "microsoft";

type ProviderConfig = {
  authUrl: string;
  tokenUrl: string;
  scope: string;
  tokenField: "id_token" | "access_token";
  tokenMethod: "POST" | "GET";
  extraAuthParams?: Record<string, string>;
};

const PROVIDER_CONFIGS: Record<OAuthProvider, ProviderConfig> = {
  google: {
    authUrl: "https://accounts.google.com/o/oauth2/v2/auth",
    tokenUrl: "https://oauth2.googleapis.com/token",
    scope: "openid email profile",
    tokenField: "id_token",
    tokenMethod: "POST",
  },
  microsoft: {
    authUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/authorize",
    tokenUrl: "https://login.microsoftonline.com/common/oauth2/v2.0/token",
    scope: "openid email profile https://graph.microsoft.com/User.Read",
    tokenField: "access_token",
    tokenMethod: "POST",
    extraAuthParams: { response_mode: "query" },
  },
  facebook: {
    authUrl: "https://www.facebook.com/v18.0/dialog/oauth",
    tokenUrl: "https://graph.facebook.com/v18.0/oauth/access_token",
    scope: "email",
    tokenField: "access_token",
    tokenMethod: "GET",
  },
};

const VERIFIER_KEY = "curralzap_oauth_code_verifier";

function generateRandomBase64Url(byteLength: number): string {
  const buffer = new Uint8Array(byteLength);
  crypto.getRandomValues(buffer);
  return base64UrlEncode(buffer);
}

async function computeCodeChallenge(verifier: string): Promise<string> {
  const data = new TextEncoder().encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return base64UrlEncode(new Uint8Array(digest));
}

function base64UrlEncode(buffer: Uint8Array): string {
  return btoa(String.fromCharCode(...buffer))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=/g, "");
}

export function consumeCodeVerifier(): string | null {
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  if (verifier != null) sessionStorage.removeItem(VERIFIER_KEY);
  return verifier;
}

export async function buildOAuthUrl(
  provider: OAuthProvider,
  clientId: string,
  redirectUri: string
): Promise<string> {
  const verifier = generateRandomBase64Url(32);
  const challenge = await computeCodeChallenge(verifier);

  sessionStorage.setItem(VERIFIER_KEY, verifier);

  const config = PROVIDER_CONFIGS[provider];
  const params = new URLSearchParams({
    client_id: clientId,
    redirect_uri: redirectUri,
    response_type: "code",
    scope: config.scope,
    code_challenge: challenge,
    code_challenge_method: "S256",
    state: provider,
    ...config.extraAuthParams,
  });

  return `${config.authUrl}?${params}`;
}

export async function exchangeOAuthCode(
  provider: OAuthProvider,
  code: string,
  clientId: string,
  redirectUri: string
): Promise<string> {
  const verifier = sessionStorage.getItem(VERIFIER_KEY);
  if (!verifier) {
    throw new Error("Missing PKCE code verifier.");
  }

  const config = PROVIDER_CONFIGS[provider];
  let response: Response;

  if (config.tokenMethod === "GET") {
    const params = new URLSearchParams({ code, client_id: clientId, redirect_uri: redirectUri, code_verifier: verifier });
    response = await fetch(`${config.tokenUrl}?${params}`);
  } else {
    response = await fetch(config.tokenUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        code,
        client_id: clientId,
        redirect_uri: redirectUri,
        code_verifier: verifier,
      }),
    });
  }

  if (!response.ok) {
    throw new Error(`OAuth token exchange failed (${response.status}).`);
  }

  const tokens = (await response.json()) as Record<string, string>;
  const token = tokens[config.tokenField];
  if (!token) {
    throw new Error(`OAuth response missing ${config.tokenField}.`);
  }

  sessionStorage.removeItem(VERIFIER_KEY);

  return token;
}
