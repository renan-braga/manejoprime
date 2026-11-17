import "@testing-library/jest-dom/vitest";
import { cleanup, configure } from "@testing-library/react";
import { afterAll, afterEach, vi } from "vitest";

configure({ asyncUtilTimeout: 5000 });

global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};

Object.defineProperty(window, "matchMedia", {
  writable: true,
  value: (query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addListener: () => {},
    removeListener: () => {},
    addEventListener: () => {},
    removeEventListener: () => {},
    dispatchEvent: () => false
  })
});

afterEach(async () => {
  cleanup();
  // Flush pending macrotasks (React 18 uses MessageChannel for scheduling)
  // so deferred transitions don't fire after globals are cleared.
  await new Promise<void>((resolve) => setTimeout(resolve, 0));
  window.localStorage.clear();
  window.sessionStorage.clear();
  vi.restoreAllMocks();
  vi.unstubAllGlobals();
});

afterAll(() => {
  if (typeof global.gc === "function") {
    global.gc();
  }
});
