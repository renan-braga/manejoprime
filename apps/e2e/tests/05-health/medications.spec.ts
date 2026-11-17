import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Medications', () => {
  test('register medication', async ({ request }) => {
    await allure.label('suite', 'Health');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const res = await request.post(`${API_URL}/api/v1/medications`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID,
        medicationName: 'Ivermectina',
        activeIngredient: 'Ivermectin',
        reason: 'Controle de parasitas — E2E',
        dosage: '1ml/10kg',
        applicationDate: new Date().toISOString().split('T')[0],
        withdrawalPeriodDays: 30,
      },
    });
    await assertOk(res, 'POST /medications');
    const body = await res.json();
    process.env.E2E_MEDICATION_ID = body.id;
    expect(body.id).toBeTruthy();
  });

  test('list medications', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.get(`${API_URL}/api/v1/medications`, {
      headers: authedHeaders(token()),
      params: { animalId: process.env.E2E_ANIMAL_ID },
    });
    await assertOk(res, 'GET /medications');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('delete medication', async ({ request }) => {
    test.skip(!process.env.E2E_MEDICATION_ID, 'Medication not created');
    const res = await request.delete(
      `${API_URL}/api/v1/medications/${process.env.E2E_MEDICATION_ID}`,
      { headers: authedHeaders(token()) }
    );
    expect([200, 204]).toContain(res.status());
  });
});
