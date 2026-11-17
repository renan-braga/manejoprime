import { APIRequestContext } from '@playwright/test';
import { API_URL, WA_WEBHOOK_SECRET, authedHeaders } from './api';

const WEBHOOK_PATH = '/api/v1/whatsapp/webhooks/evolution';
const MESSAGES_PATH = '/api/v1/whatsapp/messages';

export async function postWebhook(
  request: APIRequestContext,
  payload: object
): Promise<{ providerMessageId: string }> {
  const res = await request.post(`${API_URL}${WEBHOOK_PATH}`, {
    data: payload,
    headers: { 'X-Webhook-Secret': WA_WEBHOOK_SECRET },
  });

  if (!res.ok()) {
    throw new Error(`Webhook POST failed: ${res.status()} ${await res.text()}`);
  }

  const body = await res.json();
  return { providerMessageId: body.providerMessageId ?? (payload as any).data?.key?.id };
}

export async function pollMessageByPhone(
  request: APIRequestContext,
  token: string,
  phone: string,
  expectedStatus: string,
  timeout = 60_000
): Promise<any> {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const res = await request.get(`${API_URL}${MESSAGES_PATH}`, {
      headers: authedHeaders(token),
      params: { phone, limit: 5 },
    });
    if (res.ok()) {
      const body = await res.json();
      const items: any[] = body.items ?? body.content ?? body ?? [];
      const match = items.find((m: any) => m.status?.toLowerCase() === expectedStatus.toLowerCase());
      if (match) return match;
    }
    await new Promise(r => setTimeout(r, 1500));
  }
  throw new Error(`Timeout: no message for phone ${phone} reached status '${expectedStatus}' within ${timeout}ms`);
}

export async function pollMessageById(
  request: APIRequestContext,
  token: string,
  messageId: string,
  expectedStatus: string,
  timeout = 60_000
): Promise<any> {
  const deadline = Date.now() + timeout;
  while (Date.now() < deadline) {
    const res = await request.get(`${API_URL}${MESSAGES_PATH}/${messageId}`, {
      headers: authedHeaders(token),
    });
    if (res.ok()) {
      const body = await res.json();
      if (body.status?.toLowerCase() === expectedStatus.toLowerCase()) return body;
    }
    await new Promise(r => setTimeout(r, 1500));
  }
  throw new Error(`Timeout: message ${messageId} never reached status '${expectedStatus}' within ${timeout}ms`);
}

export function buildTextPayload(phone: string, text: string) {
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_TEXT_${Date.now()}_${Math.random().toString(36).slice(2)}`,
      },
      message: { conversation: text },
      messageType: 'conversation',
    },
  };
}

export function buildAudioPayload(phone: string, audioBase64: string) {
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_AUDIO_${Date.now()}_${Math.random().toString(36).slice(2)}`,
      },
      message: {
        base64: audioBase64,
        audioMessage: { mimetype: 'audio/ogg; codecs=opus' },
      },
      messageType: 'audioMessage',
    },
  };
}

export function buildButtonPayload(phone: string, action: 'sim' | 'nao', messageId: string) {
  const selectedButtonId = action === 'sim' ? `confirm:${messageId}` : `cancel:${messageId}`;
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_BTN_${Date.now()}_${Math.random().toString(36).slice(2)}`,
      },
      message: {
        buttonsResponseMessage: { selectedButtonId },
      },
      messageType: 'buttonsResponseMessage',
    },
  };
}
