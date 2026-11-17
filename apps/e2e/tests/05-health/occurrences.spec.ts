import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Occurrences', () => {
  let occurrenceId: string;

  test('create occurrence', async ({ request }) => {
    await allure.label('suite', 'Health');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const res = await request.post(`${API_URL}/api/v1/occurrences`, {
      headers: authedHeaders(token()),
      data: {
        animalId: process.env.E2E_ANIMAL_ID,
        type: 'health',
        severity: 'high',
        description: 'Ferida na pata dianteira direita identificada no E2E',
      },
    });
    await assertOk(res, 'POST /occurrences');
    const body = await res.json();
    occurrenceId = body.id;
    process.env.E2E_OCCURRENCE_ID = occurrenceId;
    expect(occurrenceId).toBeTruthy();
  });

  test('list occurrences', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/occurrences`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /occurrences');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('resolve occurrence', async ({ request }) => {
    test.skip(!process.env.E2E_OCCURRENCE_ID, 'Occurrence not created');
    const res = await request.post(
      `${API_URL}/api/v1/occurrences/${process.env.E2E_OCCURRENCE_ID}/resolve`,
      { headers: authedHeaders(token()) }
    );
    await assertOk(res, 'POST /occurrences/:id/resolve');
  });

  test('reopen occurrence', async ({ request }) => {
    test.skip(!process.env.E2E_OCCURRENCE_ID, 'Occurrence not created');
    const res = await request.post(
      `${API_URL}/api/v1/occurrences/${process.env.E2E_OCCURRENCE_ID}/reopen`,
      { headers: authedHeaders(token()) }
    );
    await assertOk(res, 'POST /occurrences/:id/reopen');
  });

  test('delete occurrence', async ({ request }) => {
    test.skip(!process.env.E2E_OCCURRENCE_ID, 'Occurrence not created');
    const res = await request.delete(
      `${API_URL}/api/v1/occurrences/${process.env.E2E_OCCURRENCE_ID}`,
      { headers: authedHeaders(token()) }
    );
    expect([200, 204]).toContain(res.status());
  });
});
