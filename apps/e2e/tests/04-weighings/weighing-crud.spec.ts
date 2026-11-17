import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Weighings', () => {
  test('register routine weighing', async ({ request }) => {
    await allure.label('suite', 'Weighings');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const res = await request.post(`${API_URL}/api/v1/weighings`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID,
        weightKg: 450,
        weighingDate: new Date().toISOString().split('T')[0],
        type: 'ROUTINE',
      },
    });
    await assertOk(res, 'POST /weighings (ROUTINE)');
    const body = await res.json();
    expect(body.id).toBeTruthy();
    process.env.E2E_WEIGHING_ID = body.id;
  });

  test('register exit weighing', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID_2, 'Second animal not created');

    const res = await request.post(`${API_URL}/api/v1/weighings`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID_2,
        weightKg: 380,
        weighingDate: new Date().toISOString().split('T')[0],
        type: 'EXIT',
      },
    });
    await assertOk(res, 'POST /weighings (EXIT)');
  });

  test('list weighings', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.get(`${API_URL}/api/v1/weighings`, {
      headers: authedHeaders(token()),
      params: { animalId: process.env.E2E_ANIMAL_ID },
    });
    await assertOk(res, 'GET /weighings');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
    expect(items.length).toBeGreaterThan(0);
  });
});
