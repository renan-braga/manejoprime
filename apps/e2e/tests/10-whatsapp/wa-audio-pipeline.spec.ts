import { test, expect } from '@playwright/test';
import { readdirSync, readFileSync } from 'fs';
import { join } from 'path';
import { API_URL, authedHeaders, SKIP_AUDIO } from '../../fixtures/api';
import { token, phone } from '../../fixtures/context';
import {
  postWebhook,
  pollMessageByPhone,
  pollMessageById,
  buildAudioPayload,
  buildButtonPayload,
} from '../../fixtures/whatsapp';
import * as allure from 'allure-js-commons';

const AUDIO_DIR = join(__dirname, '../../fixtures/audio');

function pickRandomAudio(operation: string): { filename: string; base64: string } | null {
  const dir = join(AUDIO_DIR, operation);
  let files: string[];
  try {
    files = readdirSync(dir).filter(f => f.endsWith('.ogg'));
  } catch {
    files = [];
  }
  if (files.length === 0) return null;
  const filename = files[Math.floor(Math.random() * files.length)];
  const base64 = readFileSync(join(dir, filename)).toString('base64');
  return { filename, base64 };
}

test.describe('WhatsApp — Audio Pipeline (full E2E)', () => {
  test.skip(SKIP_AUDIO, 'Audio tests skipped (E2E_SKIP_AUDIO=true)');

  test('pesagem via audio OGG → transcode → transcribe → intent → AWAITING_CONFIRMATION → APPLIED', async ({ request }) => {
    await allure.label('suite', 'WhatsApp');
    await allure.label('story', 'Weighing via audio');

    const fixture = pickRandomAudio('pesagem');
    test.skip(!fixture, 'No OGG fixtures for pesagem — run: npm run generate-audio');
    const { filename, base64 } = fixture!;
    await allure.attachment('audio-fixture', filename, { contentType: 'text/plain' });

    const testPhone = phone();

    // 1. POST audio webhook (base64 inline — Baileys mode, no Evolution API download needed)
    await postWebhook(request, buildAudioPayload(testPhone, base64));

    // 2. Poll through full pipeline: RECEIVED → MEDIA_DOWNLOADED → TRANSCODED → TRANSCRIBED → INTENT_EXTRACTED → AWAITING_CONFIRMATION
    const msg = await pollMessageByPhone(
      request, token(), testPhone, 'awaiting_confirmation',
      90_000  // AI pipeline can take up to 90s
    );
    await allure.step('Pipeline reached AWAITING_CONFIRMATION', async () => {
      expect(msg.status).toBe('awaiting_confirmation');
    });

    // 3. Confirm via button
    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));

    // 4. Poll until APPLIED
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 30_000);
    expect(applied.status).toBe('applied');

    // 5. Verify weighing created via API
    const weighings = await request.get(`${API_URL}/api/v1/weighings`, {
      headers: authedHeaders(token()),
    });
    const body = await weighings.json();
    const items = body.weighings ?? body.content ?? body;
    const found = items.some((w: any) => w.source === 'WHATSAPP');
    await allure.step('Verify weighing record created', async () => {
      expect(found).toBe(true);
    });
  });

  test('ocorrência via audio OGG → AWAITING_CONFIRMATION → APPLIED', async ({ request }) => {
    await allure.label('story', 'Occurrence via audio');

    const fixture = pickRandomAudio('ocorrencia');
    test.skip(!fixture, 'No OGG fixtures for ocorrencia — run: npm run generate-audio');
    const { filename, base64 } = fixture!;
    await allure.attachment('audio-fixture', filename, { contentType: 'text/plain' });

    const testPhone = phone();
    await postWebhook(request, buildAudioPayload(testPhone, base64));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 30_000);
    expect(applied.status).toBe('applied');
  });

  test('vacina via audio OGG → AWAITING_CONFIRMATION → APPLIED', async ({ request }) => {
    await allure.label('story', 'Vaccine via audio');

    const fixture = pickRandomAudio('vacina');
    test.skip(!fixture, 'No OGG fixtures for vacina — run: npm run generate-audio');
    const { filename, base64 } = fixture!;
    await allure.attachment('audio-fixture', filename, { contentType: 'text/plain' });

    const testPhone = phone();
    await postWebhook(request, buildAudioPayload(testPhone, base64));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 30_000);
    expect(applied.status).toBe('applied');
  });

  test('medicacao via audio OGG → AWAITING_CONFIRMATION → reject → REJECTED', async ({ request }) => {
    await allure.label('story', 'Medication via audio — rejected');

    const fixture = pickRandomAudio('medicacao');
    test.skip(!fixture, 'No OGG fixtures for medicacao — run: npm run generate-audio');
    const { filename, base64 } = fixture!;
    await allure.attachment('audio-fixture', filename, { contentType: 'text/plain' });

    const testPhone = phone();
    await postWebhook(request, buildAudioPayload(testPhone, base64));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    await postWebhook(request, buildButtonPayload(testPhone, 'nao', msg.id));
    const rejected = await pollMessageById(request, token(), msg.id, 'rejected', 20_000);
    expect(rejected.status).toBe('rejected');
  });

  test('alimentacao via audio OGG → AWAITING_CONFIRMATION → APPLIED', async ({ request }) => {
    await allure.label('story', 'Feeding via audio');

    const fixture = pickRandomAudio('alimentacao');
    test.skip(!fixture, 'No OGG fixtures for alimentacao — run: npm run generate-audio');
    const { filename, base64 } = fixture!;
    await allure.attachment('audio-fixture', filename, { contentType: 'text/plain' });

    const testPhone = phone();
    await postWebhook(request, buildAudioPayload(testPhone, base64));

    const msg = await pollMessageByPhone(request, token(), testPhone, 'awaiting_confirmation', 90_000);
    expect(msg.status).toBe('awaiting_confirmation');

    await postWebhook(request, buildButtonPayload(testPhone, 'sim', msg.id));
    const applied = await pollMessageById(request, token(), msg.id, 'applied', 30_000);
    expect(applied.status).toBe('applied');
  });

  test('retry failed message', async ({ request }) => {
    await allure.label('story', 'Retry failed message');

    const list = await request.get(`${API_URL}/api/v1/whatsapp/messages`, {
      headers: authedHeaders(token()),
      params: { status: 'failed', limit: 1 },
    });
    const body = await list.json();
    const items = body.messages ?? body.content ?? body;

    test.skip(!items?.length, 'No FAILED messages to retry');

    const failedId = items[0].id;
    const res = await request.post(`${API_URL}/api/v1/whatsapp/messages/${failedId}/retry`, {
      headers: authedHeaders(token()),
    });
    expect([200, 202, 204]).toContain(res.status());
  });
});
