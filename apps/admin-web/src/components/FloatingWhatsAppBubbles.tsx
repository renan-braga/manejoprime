import { useState } from "react";
import { createPortal } from "react-dom";
import { X, MessageCircle } from "lucide-react";
import { SUPPORT_WHATSAPP_URL } from "@/config/support";

const VOICE_COMMANDS = [
  { emoji: "⚖️", label: "Pesagem", example: '"Boi 45 pesou 380 kg"' },
  { emoji: "🩺", label: "Ocorrência", example: '"Animal 12 está mancando"' },
  { emoji: "🌾", label: "Alimentação", example: '"Alimentação do lote A, 200 kg"' },
  { emoji: "💉", label: "Vacinação", example: '"Vacinei o animal 7 com aftosa"' },
  { emoji: "💊", label: "Medicação", example: '"Apliquei Baymec no animal 3"' },
];

const WA_WITH_GREETING = `${SUPPORT_WHATSAPP_URL}?text=${encodeURIComponent("Olá! Quero registrar dados da fazenda.")}`;

export function FloatingWhatsAppBubbles() {
  const [voiceOpen, setVoiceOpen] = useState(false);
  const [waOpen, setWaOpen] = useState(false);

  function toggleVoice() {
    setVoiceOpen((v) => !v);
    setWaOpen(false);
  }

  function toggleWa() {
    setWaOpen((v) => !v);
    setVoiceOpen(false);
  }

  return createPortal(
    <div className="fixed bottom-6 right-6 z-[9990] flex flex-col items-end gap-3">
      {/* Voice commands panel */}
      {voiceOpen && (
        <div className="w-72 rounded-2xl border border-border bg-card shadow-2xl overflow-hidden animate-in slide-in-from-bottom-2 fade-in duration-200">
          <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-muted/40">
            <span className="text-sm font-semibold">🎙️ Comandos de voz</span>
            <button
              aria-label="Fechar"
              className="rounded-md p-0.5 text-muted-foreground hover:text-foreground transition-colors"
              onClick={() => setVoiceOpen(false)}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <ul className="px-4 py-3 space-y-3 text-sm">
            {VOICE_COMMANDS.map(({ emoji, label, example }) => (
              <li key={label} className="flex items-start gap-2.5">
                <span className="text-base leading-tight">{emoji}</span>
                <div className="min-w-0">
                  <span className="font-medium">{label}</span>
                  <p className="text-xs text-muted-foreground truncate">{example}</p>
                </div>
              </li>
            ))}
          </ul>
          <p className="px-4 pb-3 text-xs text-muted-foreground">
            Envie um áudio dizendo <span className="font-medium">"ajuda"</span> para receber essa lista no WhatsApp.
          </p>
        </div>
      )}

      {/* WhatsApp panel */}
      {waOpen && (
        <div className="w-64 rounded-2xl border border-border bg-card shadow-2xl overflow-hidden animate-in slide-in-from-bottom-2 fade-in duration-200">
          <div className="flex items-center justify-between px-4 py-3 border-b border-border bg-green-500/10">
            <span className="text-sm font-semibold text-green-700 dark:text-green-400">
              WhatsApp ManejoPrime
            </span>
            <button
              aria-label="Fechar"
              className="rounded-md p-0.5 text-muted-foreground hover:text-foreground transition-colors"
              onClick={() => setWaOpen(false)}
              type="button"
            >
              <X className="h-4 w-4" />
            </button>
          </div>
          <div className="px-4 py-3 space-y-3">
            <p className="text-xs text-muted-foreground">
              Envie áudios ou textos para registrar dados da fazenda diretamente pelo WhatsApp.
            </p>
            <a
              className="flex items-center justify-center gap-2 w-full rounded-lg bg-green-500 text-white px-3 py-2 text-sm font-medium hover:bg-green-600 transition-colors"
              href={WA_WITH_GREETING}
              rel="noopener noreferrer"
              target="_blank"
            >
              <MessageCircle className="h-4 w-4" />
              Abrir no WhatsApp
            </a>
          </div>
        </div>
      )}

      {/* FAB stack */}
      <div className="flex flex-col items-end gap-2.5">
        <button
          aria-label="Comandos de voz pelo WhatsApp"
          className={`h-12 w-12 rounded-full shadow-lg text-lg flex items-center justify-center transition-all hover:scale-105 active:scale-95 ${
            voiceOpen
              ? "bg-primary text-primary-foreground ring-2 ring-primary/30"
              : "bg-card border border-border hover:bg-muted"
          }`}
          onClick={toggleVoice}
          type="button"
        >
          🎙️
        </button>

        <button
          aria-label="Abrir WhatsApp ManejoPrime"
          className={`h-12 w-12 rounded-full shadow-lg flex items-center justify-center transition-all hover:scale-105 active:scale-95 ${
            waOpen
              ? "bg-green-600 text-white ring-2 ring-green-400/30"
              : "bg-green-500 text-white hover:bg-green-600"
          }`}
          onClick={toggleWa}
          type="button"
        >
          <MessageCircle className="h-5 w-5" />
        </button>
      </div>
    </div>,
    document.body
  );
}
