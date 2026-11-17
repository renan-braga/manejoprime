const DEFAULT_API_BASE_URL = "http://127.0.0.1:8081";

export function readApiBaseUrl() {
  const runtimeApiBaseUrl =
    typeof window === "undefined" ? undefined : window.CURRALZAP_RUNTIME_CONFIG?.apiBaseUrl;

  return normalizeApiBaseUrl(runtimeApiBaseUrl ?? import.meta.env.VITE_API_BASE_URL ?? DEFAULT_API_BASE_URL);
}

export function readGoogleClientId() {
  return window.CURRALZAP_RUNTIME_CONFIG?.googleClientId ?? import.meta.env.VITE_GOOGLE_CLIENT_ID ?? "";
}

export function readFacebookAppId() {
  return window.CURRALZAP_RUNTIME_CONFIG?.facebookAppId ?? import.meta.env.VITE_FACEBOOK_APP_ID ?? "";
}

export function readMicrosoftClientId() {
  return window.CURRALZAP_RUNTIME_CONFIG?.microsoftClientId ?? import.meta.env.VITE_MICROSOFT_CLIENT_ID ?? "";
}

function normalizeApiBaseUrl(value: string) {
  return value.replace(/\/$/, "");
}
