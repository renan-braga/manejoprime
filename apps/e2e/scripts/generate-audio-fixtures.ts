/**
 * Gera fixtures de áudio para os testes E2E usando a API TTS da Groq.
 *
 * Uso:
 *   GROQ_API_KEY=gsk_... npx ts-node scripts/generate-audio-fixtures.ts
 *
 * Os arquivos gerados são commitados no repo. CI usa esses arquivos sem
 * precisar de GROQ_API_KEY.
 *
 * Para regenerar (ex.: melhorar frases ou adicionar vozes):
 *   GROQ_API_KEY=gsk_... npx ts-node scripts/generate-audio-fixtures.ts
 */

import { writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const GROQ_API_KEY = process.env.GROQ_API_KEY;
if (!GROQ_API_KEY) {
  console.error('GROQ_API_KEY not set. Export it before running this script.');
  process.exit(1);
}

// Vozes disponíveis no Groq TTS (rotação entre variações)
const VOICES = ['Arista-PlayAI', 'Atlas-PlayAI', 'Basil-PlayAI', 'Briggs-PlayAI', 'Calum-PlayAI', 'Celeste-PlayAI', 'Cheyenne-PlayAI', 'Chip-PlayAI', 'Cillian-PlayAI', 'Deedee-PlayAI'] as const;

type Voice = typeof VOICES[number];

interface Fixture {
  operation: string;
  phrases: string[];
}

const FIXTURES: Fixture[] = [
  {
    operation: 'pesagem',
    phrases: [
      'pesagem boi brinco cento e vinte e três quatrocentos e cinquenta quilos',
      'boi cento e vinte e três pesou quatrocentos e cinquenta quilos',
      'registra pesagem brinco cento e vinte e três peso quatrocentos e cinquenta',
      'anotei o brinco cento e vinte e três na balança deu quatrocentos e cinquenta',
      'pesagem de hoje boi cento e vinte e três marcou quatrocentos e cinquenta quilos',
      'tô mandando pesagem do cento e vinte e três quatrocentos e cinquenta quilos',
      'peso do animal brinco cento e vinte e três está em quatrocentos e cinquenta',
      'o cento e vinte e três pesou hoje quatrocentos e cinquenta quilos na balança',
      'quatrocentos e cinquenta quilos boi brinco cento e vinte e três',
      'balança marcou quatrocentos e cinquenta no boi cento e vinte e três',
    ],
  },
  {
    operation: 'ocorrencia',
    phrases: [
      'boi brinco duzentos com ferida na pata dianteira direita',
      'animal duzentos com lesão na perna da frente direita',
      'brinco duzentos apresentando ferida na pata dianteira',
      'vaca duzentos tem uma ferida na pata da frente direita',
      'anotei ocorrência no duzentos ferida pata dianteira direita',
      'o boi duzentos tá com ferida na pata da frente do lado direito',
      'registra ocorrência duzentos ferimento pata dianteira direita',
      'vi que o duzentos tem uma ferida na perna dianteira direita',
      'brinco duzentos ferida grave na pata dianteira direita',
      'ocorrência boi duzentos lesão na pata frente direita',
    ],
  },
  {
    operation: 'vacina',
    phrases: [
      'vacina aftosa dose única brinco trezentos e quarenta e cinco',
      'brinco trezentos e quarenta e cinco vacinado aftosa primeira dose',
      'apliquei vacina aftosa no boi trezentos e quarenta e cinco',
      'vacinei o trezentos e quarenta e cinco com aftosa dose um',
      'vacina febre aftosa brinco trezentos e quarenta e cinco primeira dose',
      'registra vacina aftosa no animal trezentos e quarenta e cinco dose um',
      'boi trezentos e quarenta e cinco recebeu aftosa hoje primeira dose',
      'trezentos e quarenta e cinco vacinado contra aftosa dose inicial',
      'vacina aftosa aplicada no brinco trezentos e quarenta e cinco',
      'aplicação vacina aftosa boi trezentos e quarenta e cinco primeira dose',
    ],
  },
  {
    operation: 'medicacao',
    phrases: [
      'ivermectina subcutânea brinco cento e vinte e três',
      'apliquei ivermectina no boi cento e vinte e três via subcutânea',
      'brinco cento e vinte e três recebeu ivermectina subcutânea hoje',
      'medicação ivermectina boi cento e vinte e três aplicação subcutânea',
      'tratei o cento e vinte e três com ivermectina subcutânea',
      'boi cento e vinte e três ivermectina subcutânea hoje',
      'apliquei ivermec no brinco cento e vinte e três via sub',
      'cento e vinte e três tratamento ivermectina via subcutânea',
      'registra ivermectina subcutânea no animal cento e vinte e três',
      'medicou o cento e vinte e três com ivermectina subcutânea',
    ],
  },
  {
    operation: 'alimentacao',
    phrases: [
      'alimentação silagem de milho dez quilos lote A',
      'forneci dez quilos de silagem de milho pro lote A',
      'lote A recebeu dez quilos de silagem de milho hoje',
      'fornecimento silagem milho dez quilos animais lote A',
      'alimentei o lote A com dez quilos de silagem de milho',
      'dez quilos silagem de milho fornecidos pro lote A',
      'registro alimentação lote A silagem de milho dez quilos',
      'lote A alimentação de hoje dez quilos silagem milho',
      'coloquei dez quilos de silagem pro lote A',
      'alimentação do dia lote A dez quilos silagem de milho',
    ],
  },
];

async function generateAudio(text: string, voice: Voice): Promise<Buffer> {
  const response = await fetch('https://api.groq.com/openai/v1/audio/speech', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${GROQ_API_KEY}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      model: 'playai-tts',
      input: text,
      voice,
      response_format: 'opus',
    }),
  });

  if (!response.ok) {
    const err = await response.text();
    throw new Error(`Groq TTS error ${response.status}: ${err}`);
  }

  const arrayBuffer = await response.arrayBuffer();
  return Buffer.from(arrayBuffer);
}

async function main() {
  const outputBase = join(__dirname, '..', 'fixtures', 'audio');

  for (const fixture of FIXTURES) {
    const dir = join(outputBase, fixture.operation);
    mkdirSync(dir, { recursive: true });

    console.log(`\n[${fixture.operation}] Generating ${fixture.phrases.length} audio fixtures...`);

    for (let i = 0; i < fixture.phrases.length; i++) {
      const phrase = fixture.phrases[i];
      const voice = VOICES[i % VOICES.length];
      const filename = `v${String(i + 1).padStart(2, '0')}_${voice.toLowerCase().replace('-', '_')}.ogg`;
      const outputPath = join(dir, filename);

      process.stdout.write(`  [${i + 1}/${fixture.phrases.length}] ${voice}: "${phrase.substring(0, 50)}..." `);

      try {
        const audio = await generateAudio(phrase, voice);
        writeFileSync(outputPath, audio);
        console.log(`✓ (${audio.length} bytes)`);
      } catch (err) {
        console.error(`✗ FAILED: ${err}`);
        throw err;
      }

      // Small delay to avoid rate limiting
      if (i < fixture.phrases.length - 1) {
        await new Promise(r => setTimeout(r, 200));
      }
    }

    console.log(`[${fixture.operation}] Done.`);
  }

  console.log('\nAll fixtures generated. Commit the apps/e2e/fixtures/audio/ directory.');
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
