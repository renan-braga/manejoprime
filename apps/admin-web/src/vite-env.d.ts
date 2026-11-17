/// <reference types="vite/client" />

declare global {
  interface Window {
    CURRALZAP_RUNTIME_CONFIG?: {
      apiBaseUrl?: string;
      googleClientId?: string;
      facebookAppId?: string;
      microsoftClientId?: string;
    };
  }
}

export {};
