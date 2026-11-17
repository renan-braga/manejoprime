import { readdirSync, readFileSync } from 'fs';
import { join } from 'path';

const AUDIO_DIR = join(__dirname, 'audio');

export type AudioOperation = 'pesagem' | 'ocorrencia' | 'vacina' | 'medicacao' | 'alimentacao';

export function pickRandomAudio(operation: AudioOperation): { filename: string; base64: string } {
  const dir = join(AUDIO_DIR, operation);
  const files = readdirSync(dir).filter(f => f.endsWith('.ogg'));

  if (files.length === 0) {
    throw new Error(
      `No OGG fixtures found in ${dir}. Run: npm run generate-audio (requires GROQ_API_KEY)`
    );
  }

  const filename = files[Math.floor(Math.random() * files.length)];
  const base64 = readFileSync(join(dir, filename)).toString('base64');
  return { filename, base64 };
}

export function buildAudioWebhookPayload(phone: string, base64: string) {
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_AUDIO_${Date.now()}`,
      },
      message: {
        base64,
        audioMessage: { mimetype: 'audio/ogg; codecs=opus' },
      },
      messageType: 'audioMessage',
    },
  };
}

export function buildTextWebhookPayload(phone: string, text: string) {
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_TEXT_${Date.now()}`,
      },
      message: { conversation: text },
      messageType: 'conversation',
    },
  };
}

export function buildButtonResponsePayload(phone: string, action: 'sim' | 'nao', messageId: string) {
  const selectedButtonId = action === 'sim' ? `confirm:${messageId}` : `cancel:${messageId}`;
  return {
    event: 'messages.upsert',
    data: {
      key: {
        remoteJid: `${phone}@s.whatsapp.net`,
        fromMe: false,
        id: `wamid.E2E_BTN_${Date.now()}`,
      },
      message: {
        buttonsResponseMessage: { selectedButtonId },
      },
      messageType: 'buttonsResponseMessage',
    },
  };
}
