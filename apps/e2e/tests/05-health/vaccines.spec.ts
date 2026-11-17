import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Vaccines', () => {
  test('register vaccine', async ({ request }) => {
    await allure.label('suite', 'Health');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const res = await request.post(`${API_URL}/api/v1/vaccines`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID,
        vaccineType: 'Aftosa',
        dose: 'primeira',
        applicationDate: new Date().toISOString().split('T')[0],
        volumeMl: 2,
      },
    });
    await assertOk(res, 'POST /vaccines');
    const body = await res.json();
    process.env.E2E_VACCINE_ID = body.id;
    expect(body.id).toBeTruthy();
  });

  test('list vaccines', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.get(`${API_URL}/api/v1/vaccines`, {
      headers: authedHeaders(token()),
      params: { animalId: process.env.E2E_ANIMAL_ID },
    });
    await assertOk(res, 'GET /vaccines');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('delete vaccine', async ({ request }) => {
    test.skip(!process.env.E2E_VACCINE_ID, 'Vaccine not created');
    const res = await request.delete(
      `${API_URL}/api/v1/vaccines/${process.env.E2E_VACCINE_ID}`,
      { headers: authedHeaders(token()) }
    );
    expect([200, 204]).toContain(res.status());
  });
});
