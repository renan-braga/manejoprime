import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token, farmId } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Farms', () => {
  test('list my farms', async ({ request }) => {
    await allure.label('suite', 'Farms');
    const res = await request.get(`${API_URL}/api/v1/my-farms`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /my-farms');
    const body = await res.json();
    const farms = body.farms ?? body;
    expect(Array.isArray(farms)).toBe(true);
    expect(farms.some((f: any) => f.farmId === farmId())).toBe(true);
  });

  test('create second farm', async ({ request }) => {
    await allure.label('story', 'Create farm');
    const res = await request.post(`${API_URL}/api/v1/my-farms`, {
      headers: authedHeaders(token()),
      data: { farmName: 'E2E Segunda Fazenda' },
    });
    await assertOk(res, 'POST /my-farms');
    const body = await res.json();
    expect(body.id ?? body.farmId).toBeTruthy();
    process.env.E2E_SECOND_FARM_ID = body.id ?? body.farmId;
  });

  test('switch to second farm', async ({ request }) => {
    await allure.label('story', 'Switch farm');
    const secondFarmId = process.env.E2E_SECOND_FARM_ID;
    test.skip(!secondFarmId, 'Second farm not created');

    const res = await request.post(`${API_URL}/api/v1/auth/switch-farm/${secondFarmId}`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'POST /auth/switch-farm');
    const body = await res.json();
    expect(body.accessToken).toBeTruthy();
    // Switch back to primary farm
    await request.post(`${API_URL}/api/v1/auth/switch-farm/${farmId()}`, {
      headers: authedHeaders(token()),
    });
  });

  test('read and update farm settings', async ({ request }) => {
    await allure.label('story', 'Farm settings');
    const get = await request.get(`${API_URL}/api/v1/settings`, {
      headers: authedHeaders(token()),
    });
    await assertOk(get, 'GET /settings');

    const patch = await request.patch(`${API_URL}/api/v1/settings`, {
      headers: authedHeaders(token()),
      data: { farmName: 'E2E Fazenda Atualizada', carcassYieldPercent: 52 },
    });
    await assertOk(patch, 'PATCH /settings');
  });
});
