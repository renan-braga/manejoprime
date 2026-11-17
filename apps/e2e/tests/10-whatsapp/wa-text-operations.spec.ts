import { test, expect } from '@playwright/test';
import { API_URL, authedHeaders, WA_WEBHOOK_SECRET } from '../../fixtures/api';
import { token, phone } from '../../fixtures/context';
import {
  postWebhook,
  pollMessageByPhone,
  pollMessageById,
  buildTextPayload,
  buildButtonPayload,
} from '../../fixtures/whatsapp';
import * as allure from 'allure-js-commons';

test.describe('WhatsApp — Text Operations', () => {
  // multi-farm runs first so the WhatsappFarmSession is created before the operation tests.
  // Without the session, messages from users with 2+ farms go to PENDING_FARM_SELECTION
  // instead of the processing pipeline, causing pesagem/ocorrência/vacina tests to time out.
  test('multi-farm session — unknown phone gets selection menu', async ({ request }) => {
    await allure.label('story', 'Multi-farm selection');
    // This test only works if the user has 2 farms — skip if not
    const farmsRes = await request.get(`${API_URL}/api/v1/my-farms`, {
      headers: authedHeaders(token()),
    });
    const farmsBody = await farmsRes.json();
    const farms = farmsBody.items ?? farmsBody.farms ?? farmsBody;
    test.skip(!Array.isArray(farms) || farms.length < 2, 'User has only one farm — multi-farm test skipped');

    const testPhone = phone();
    await postWebhook(request, buildTextPayload(testPhone, 'oi, bom dia'));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'pending_farm_selection', 30_000);
    expect(msg.status).toBe('pending_farm_selection');

    // Reply with selection "1" — selects the primary (oldest) farm, creating a session
    await postWebhook(request, buildTextPayload(testPhone, '1'));
    const selected = await pollMessageByPhone(request, token(), testPhone, 'farm_selected', 30_000);
    expect(selected.status).toBe('farm_selected');
  });

  test('pesagem via text → AWAITING_CONFIRMATION → confirm → APPLIED', async ({ request }) => {
    await allure.label('suite', 'WhatsApp');
    await allure.label('story', 'Weighing via text');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const testPhone = phone();
    const identifier = 'E2E-123';

    // 1. Send text message simulating weighing intent
    await postWebhook(request, buildTextPayload(
      testPhone,
      `pesagem boi brinco ${identifier} quatrocentos e oitenta quilos`
    ));

    // 2. Poll until AWAITING_CONFIRMATION (pipeline processed text intent)
    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    await allure.step('Pipeline reached AWAITING_CONFIRMATION', async () => {
      expect(msg.status).toBe('awaiting_confirmation');
    });

    // 3. Confirm via button response
    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));

    // 4. Poll until APPLIED
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 20_000);
    expect(applied.status).toBe('applied');

    // 5. Verify weighing created
    const weighings = await request.get(`${API_URL}/api/v1/weighings`, {
      headers: authedHeaders(token()),
      params: { animalId: process.env.E2E_ANIMAL_ID ?? '' },
    });
    const body = await weighings.json();
    const items = body.items ?? body;
    const found = items.some((w: any) => w.source === 'WHATSAPP' || w.weightKg === 480);
    expect(found).toBe(true);
  });

  test('ocorrência via text → AWAITING_CONFIRMATION', async ({ request }) => {
    await allure.label('story', 'Occurrence via text');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const testPhone = phone();
    await postWebhook(request, buildTextPayload(
      testPhone,
      'boi brinco E2E-123 com ferida na pata dianteira direita'
    ));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    // Reject this one
    await postWebhook(request, buildButtonPayload(testPhone, 'nao', msg.id));
    const rejected = await pollMessageById(request, token(), msg.id, 'rejected', 20_000);
    expect(rejected.status).toBe('rejected');
  });

  test('vacina via text → AWAITING_CONFIRMATION → confirm → APPLIED', async ({ request }) => {
    await allure.label('story', 'Vaccine via text');
    test.skip(!process.env.E2E_ANIMAL_ID, 'Animal not created');

    const testPhone = phone();
    await postWebhook(request, buildTextPayload(
      testPhone,
      'vacina aftosa primeira dose brinco E2E-123'
    ));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 20_000);
    expect(applied.status).toBe('applied');
  });

  test('list whatsapp messages', async ({ request }) => {
    const res = await request.get(`${API_URL}/api/v1/whatsapp/messages`, {
      headers: authedHeaders(token()),
    });
    expect(res.ok()).toBe(true);
    const body = await res.json();
    const items = body.items ?? body;
    expect(Array.isArray(items)).toBe(true);
    // Items may be empty if pipeline tests ran but messages weren't processed yet
  });

  test('get whatsapp message detail with processing steps', async ({ request }) => {
    const list = await request.get(`${API_URL}/api/v1/whatsapp/messages`, {
      headers: authedHeaders(token()),
    });
    const body = await list.json();
    const items = body.items ?? body;
    const firstId = items[0]?.id;
    test.skip(!firstId, 'No messages found');

    const detail = await request.get(`${API_URL}/api/v1/whatsapp/messages/${firstId}`, {
      headers: authedHeaders(token()),
    });
    expect(detail.ok()).toBe(true);
    const detailBody = await detail.json();
    expect(detailBody.id).toBe(firstId);
  });
});
