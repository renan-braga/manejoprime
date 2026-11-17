import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Feeding', () => {
  test('register feeding', async ({ request }) => {
    await allure.label('suite', 'Feeding');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const res = await request.post(`${API_URL}/api/v1/feedings`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID,
        feedType: 'silagem',
        quantityKg: 10,
        startDate: new Date().toISOString().split('T')[0],
      },
    });
    await assertOk(res, 'POST /feedings');
    const body = await res.json();
    process.env.E2E_FEEDING_ID = body.id;
    expect(body.id).toBeTruthy();
  });

  test('list feedings', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.get(`${API_URL}/api/v1/feedings`, {
      headers: authedHeaders(token()),
      params: { animalId: process.env.E2E_ANIMAL_ID },
    });
    await assertOk(res, 'GET /feedings');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });
});
