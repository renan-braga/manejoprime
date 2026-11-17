import { defineConfig } from '@playwright/test';

const BASE_URL = process.env.E2E_BASE_URL ?? 'https://manejoprime.com.br';
const API_URL = process.env.E2E_API_URL ?? 'https://api.manejoprime.com.br';

export default defineConfig({
  testDir: './tests',
  globalSetup: require.resolve('./global-setup'),
  globalTeardown: require.resolve('./global-teardown'),
  timeout: 120_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1,
  retries: 1,
  reporter: [
    ['line'],
    ['allure-playwright', {
      detail: true,
      outputFolder: 'allure-results',
      suiteTitle: true,
    }],
  ],
  use: {
    baseURL: BASE_URL,
    extraHTTPHeaders: {
      'Content-Type': 'application/json',
    },
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    {
      name: 'api',
      testMatch: /tests\/(0[0-9]|10)-.*\/.+\.spec\.ts/,
      use: {
        baseURL: API_URL,
      },
    },
    {
      name: 'ui',
      testMatch: /tests\/11-.*\/.+\.spec\.ts/,
      use: {
        browserName: 'chromium',
        baseURL: BASE_URL,
        viewport: { width: 1280, height: 720 },
      },
    },
  ],
});
