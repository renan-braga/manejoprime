import { afterEach, describe, expect, it } from "vitest";
import {
  readApiBaseUrl,
  readGoogleClientId,
  readFacebookAppId,
  readMicrosoftClientId
} from "../config/runtimeConfig";

describe("runtime config", () => {
  afterEach(() => {
    delete window.CURRALZAP_RUNTIME_CONFIG;
  });

  it("prefers the injected runtime API base URL", () => {
    window.CURRALZAP_RUNTIME_CONFIG = {
      apiBaseUrl: "https://admin-api.curralzap.test/"
    };

    expect(readApiBaseUrl()).toBe("https://admin-api.curralzap.test");
  });

  it("falls back to the default local API base URL when no runtime config is injected", () => {
    expect(readApiBaseUrl()).toBe("http://127.0.0.1:8081");
  });

  it("reads the Google client ID from runtime config", () => {
    window.CURRALZAP_RUNTIME_CONFIG = { googleClientId: "google-client-id-123" };
    expect(readGoogleClientId()).toBe("google-client-id-123");
  });

  it("returns empty string for Google client ID when not configured", () => {
    expect(readGoogleClientId()).toBe("");
  });

  it("reads the Facebook app ID from runtime config", () => {
    window.CURRALZAP_RUNTIME_CONFIG = { facebookAppId: "fb-app-id-456" };
    expect(readFacebookAppId()).toBe("fb-app-id-456");
  });

  it("returns empty string for Facebook app ID when not configured", () => {
    expect(readFacebookAppId()).toBe("");
  });

  it("reads the Microsoft client ID from runtime config", () => {
    window.CURRALZAP_RUNTIME_CONFIG = { microsoftClientId: "ms-client-id-789" };
    expect(readMicrosoftClientId()).toBe("ms-client-id-789");
  });

  it("returns empty string for Microsoft client ID when not configured", () => {
    expect(readMicrosoftClientId()).toBe("");
  });
});
