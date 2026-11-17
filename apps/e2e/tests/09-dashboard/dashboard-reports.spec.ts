import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Dashboard & Reports', () => {
  test('dashboard summary', async ({ request }) => {
    await allure.label('suite', 'Dashboard');
    const res = await request.get(`${API_URL}/api/v1/dashboard`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /dashboard');
    const body = await res.json();
    expect(body).toBeTruthy();
  });

  test('weight trend', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/dashboard/weight-trend`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /dashboard/weight-trend');
  });

  test('lot breakdown', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/dashboard/lot-breakdown`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /dashboard/lot-breakdown');
  });
});
