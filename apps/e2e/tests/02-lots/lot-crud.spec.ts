import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Lots', () => {
  test('create lot', async ({ request }) => {
    await allure.label('suite', 'Lots');
    const res = await request.post(`${API_URL}/api/v1/lots`, {
      headers: authedHeaders(token()),
      data: { name: 'Lote E2E Alpha', purpose: 'FATTENING' },
    });
    await assertOk(res, 'POST /lots');
    const body = await res.json();
    expect(body.id).toBeTruthy();
    process.env.E2E_LOT_ID = body.id;
  });

  test('list lots', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/lots`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /lots');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('get lot detail', async ({ request }) => {
    test.skip(!process.env.E2E_LOT_ID, 'Lot not created');
    const res = await request.get(`${API_URL}/api/v1/lots/${process.env.E2E_LOT_ID}`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /lots/:id');
    const body = await res.json();
    expect(body.id).toBe(process.env.E2E_LOT_ID);
  });

  test('update lot', async ({ request }) => {
    test.skip(!process.env.E2E_LOT_ID, 'Lot not created');
    const res = await request.patch(`${API_URL}/api/v1/lots/${process.env.E2E_LOT_ID}`, {
      headers: authedHeaders(token()),
      data: { name: 'Lote E2E Alpha Atualizado', purpose: 'FATTENING' },
    });
    await assertOk(res, 'PATCH /lots/:id');
  });
});
