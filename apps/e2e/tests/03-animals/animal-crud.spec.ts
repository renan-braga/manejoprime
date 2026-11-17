import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Animals', () => {
  test('create animal', async ({ request }) => {
    await allure.label('suite', 'Animals');
    const res = await request.post(`${API_URL}/api/v1/animals`, {
      headers: authedHeaders(token()),
      data: {
        identifier: 'E2E-123',
        name: 'Trovão E2E',
        breed: 'Nelore',
        sex: 'male',
        status: 'active',
        birthDate: '2022-01-15',
        birthWeightKg: 35,
        entryDate: '2023-06-01',
        lotId: process.env.E2E_LOT_ID ?? null,
      },
    });
    await assertOk(res, 'POST /animals');
    const body = await res.json();
    expect(body.id).toBeTruthy();
    process.env.E2E_ANIMAL_ID = body.id;
  });

  test('list animals', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/animals`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /animals');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('get animal detail', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.get(`${API_URL}/api/v1/animals/${process.env.E2E_ANIMAL_ID}`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /animals/:id');
    const body = await res.json();
    expect(body.id).toBe(process.env.E2E_ANIMAL_ID);
    expect(body.identifier ?? body.brinco).toBe('E2E-123');
  });

  test('update animal', async ({ request }) => {
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');
    const res = await request.patch(`${API_URL}/api/v1/animals/${process.env.E2E_ANIMAL_ID}`, {
      headers: authedHeaders(token()),
      data: { identifier: 'E2E-123', name: 'Trovão E2E Atualizado', sex: 'male', status: 'active' },
    });
    await assertOk(res, 'PATCH /animals/:id');
  });

  test('create second animal for offer tests', async ({ request }) => {
    const res = await request.post(`${API_URL}/api/v1/animals`, {
      headers: authedHeaders(token()),
      data: {
        identifier: 'E2E-456',
        name: 'Relâmpago E2E',
        breed: 'Angus',
        sex: 'female',
        status: 'active',
        birthDate: '2021-05-10',
        entryDate: '2023-07-15',
      },
    });
    await assertOk(res, 'POST /animals (second)');
    const body = await res.json();
    process.env.E2E_ANIMAL_ID_2 = body.id;
  });
});
