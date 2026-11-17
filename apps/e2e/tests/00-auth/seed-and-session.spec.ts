import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders } from '../../fixtures/api';
import { token, refreshToken, farmId, userId, phone } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Auth', () => {
  test('seed returns valid JWT and session', async ({ request }) => {
    await allure.label('suite', 'Auth');
    await allure.label('story', 'Seed endpoint');

    expect(token()).toBeTruthy();
    expect(refreshToken()).toBeTruthy();
    expect(farmId()).toMatch(/^[0-9a-f-]{36}$/);
    expect(userId()).toMatch(/^[0-9a-f-]{36}$/);
    expect(phone()).toMatch(/^\d{10,15}$/);

    const me = await request.get(`${API_URL}/api/v1/session/me`, {
      headers: authedHeaders(token()),
    });
    expect(me.ok()).toBe(true);
    const body = await me.json();
    expect(body.userId ?? body.user?.id ?? body.id).toBeTruthy();
  });

  test('refresh token flow', async ({ request }) => {
    await allure.label('story', 'Token refresh');

    const res = await request.post(`${API_URL}/api/v1/auth/refresh`, {
      data: { refreshToken: refreshToken() },
    });
    expect(res.ok()).toBe(true);
    const body = await res.json();
    expect(body.accessToken).toBeTruthy();
  });

  test('unauthenticated request returns 401', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/session/me`);
    expect(res.status()).toBe(401);
  });
});
