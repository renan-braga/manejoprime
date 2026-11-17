import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, assertOk } from '../../fixtures/api';
import { token } from '../../fixtures/context';
import * as allure from 'allure-js-commons';

test.describe('Offers', () => {
  let offerId: string;
  let offerIdForSale: string;

  test('create offer (DRAFT)', async ({ request }) => {
    await allure.label('suite', 'Offers');
    const animalIds = [
      process.env.E2E_ANIMAL_ID,
      process.env.E2E_ANIMAL_ID_2,
    ].filter(Boolean);

    const res = await request.post(`${API_URL}/api/v1/offers`, {
      headers: authedHeaders(token()),
      data: {
        title: 'Oferta E2E Teste',
        askingPrice: 5000,
        notes: 'Criada pelo suite E2E',
        animalIds,
      },
    });
    await assertOk(res, 'POST /offers');
    const body = await res.json();
    offerId = body.id;
    expect(offerId).toBeTruthy();
  });

  test('list offers', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/offers`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'GET /offers');
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
  });

  test('update offer price', async ({ request }) => {
    test.skip(!offerId, 'Offer not created');
    const res = await request.patch(`${API_URL}/api/v1/offers/${offerId}`, {
      headers: authedHeaders(token()),
      data: { title: 'Oferta E2E Teste', askingPrice: 5500, notes: 'Preço ajustado no E2E' },
    });
    await assertOk(res, 'PATCH /offers/:id');
  });

  test('activate offer', async ({ request }) => {
    test.skip(!offerId, 'Offer not created');
    const res = await request.post(`${API_URL}/api/v1/offers/${offerId}/activate`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'POST /offers/:id/activate');
  });

  test('cancel offer', async ({ request }) => {
    test.skip(!offerId, 'Offer not created');
    const res = await request.post(`${API_URL}/api/v1/offers/${offerId}/cancel`, {
      headers: authedHeaders(token()),
    });
    await assertOk(res, 'POST /offers/:id/cancel');
  });

  test('create and mark-sold offer', async ({ request }) => {
    const res = await request.post(`${API_URL}/api/v1/offers`, {
      headers: authedHeaders(token()),
      data: { title: 'Oferta Para Vender E2E', askingPrice: 3000, animalIds: [] },
    });
    await assertOk(res, 'POST /offers (for sale)');
    const body = await res.json();
    offerIdForSale = body.id;

    await request.post(`${API_URL}/api/v1/offers/${offerIdForSale}/activate`, {
      headers: authedHeaders(token()),
    });

    const sold = await request.post(`${API_URL}/api/v1/offers/${offerIdForSale}/mark-sold`, {
      headers: authedHeaders(token()),
    });
    await assertOk(sold, 'POST /offers/:id/mark-sold');
  });
});
