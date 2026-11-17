import { useState } from "react";
import { X, MessageCircle } from "lucide-react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";
import { SupportLinks } from "../../components/SupportLinks";
import { SUPPORT_WHATSAPP_URL } from "../../config/support";
import { ImportSheet } from "./ImportSheet";
import { useAuth } from "../auth/AuthContext";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import type { OnboardingStatus } from "./useOnboardingStatus";

type Step = 1 | 2 | 3;

type Props = {
  status: OnboardingStatus;
  onDismiss: () => void;
  onStatusRefresh: () => void;
};

type InviteForm = { email: string; phone: string; role: string };

export function OnboardingWizardModal({ status, onDismiss, onStatusRefresh }: Props) {
  const { fetchProtectedJson, session } = useAuth();
  const [step, setStep] = useState<Step>(1);
  const [phone, setPhone] = useState(session?.phone ?? "");
  const [phoneSaving, setPhoneSaving] = useState(false);
  const [phoneError, setPhoneError] = useState<string | null>(null);
  const [phoneSaved, setPhoneSaved] = useState(false);

  const [vetForm, setVetForm] = useState<InviteForm>({ email: "", phone: "", role: "veterinarian" });
  const [vetError, setVetError] = useState<string | null>(null);
  const [vetFieldErrors, setVetFieldErrors] = useState<Record<string, string>>({});
  const [vetSent, setVetSent] = useState(false);

  const [workerForm, setWorkerForm] = useState<InviteForm>({ email: "", phone: "", role: "worker" });
  const [workerError, setWorkerError] = useState<string | null>(null);
  const [workerFieldErrors, setWorkerFieldErrors] = useState<Record<string, string>>({});
  const [workerSent, setWorkerSent] = useState(false);

  const [hasLotsNow, setHasLotsNow] = useState(status.hasLots);
  const [hasAnimalsNow, setHasAnimalsNow] = useState(status.hasAnimals);
  const [importTab, setImportTab] = useState<"lots" | "animals" | "vaccines">("lots");

  async function savePhone() {
    if (!session?.userId) return;
    setPhoneSaving(true);
    setPhoneError(null);
    try {
      await fetchProtectedJson(`/api/v1/users/${session.userId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone }),
      });
      setPhoneSaved(true);
    } catch (err) {
      setPhoneError(describeApiError(err));
    } finally {
      setPhoneSaving(false);
    }
  }

  async function sendInvite(form: InviteForm, onSuccess: () => void, onError: (msg: string) => void, onFieldErrors: (e: Record<string, string>) => void) {
    try {
      await fetchProtectedJson("/api/v1/invites", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });
      onSuccess();
    } catch (err) {
      const fe = readFieldErrors(err);
      if (Object.keys(fe).length > 0) onFieldErrors(fe);
      else onError(describeApiError(err));
    }
  }

  function StepIndicator() {
    return (
      <div className="flex items-center gap-1.5 justify-center mb-6">
        {([1, 2, 3] as Step[]).map((s) => (
          <div
            key={s}
            className={`h-2 rounded-full transition-all ${
              s === step ? "w-6 bg-primary" : s < step ? "w-2 bg-primary/40" : "w-2 bg-muted"
            }`}
          />
        ))}
      </div>
    );
  }

  return (
    <Dialog open onOpenChange={(open) => { if (!open) onDismiss(); }}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <div className="flex items-center justify-between">
            <DialogTitle className="text-lg">
              {step === 1 && "Seu WhatsApp"}
              {step === 2 && "Sua equipe"}
              {step === 3 && "Importar dados da fazenda"}
            </DialogTitle>
            <button
              aria-label="Fechar"
              onClick={onDismiss}
              className="rounded-md p-1.5 hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
              type="button"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        </DialogHeader>

        <StepIndicator />

        {/* ── Step 1: WhatsApp ── */}
        {step === 1 && (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Seu número será usado para alertas e comandos de voz no WhatsApp do ManejoPrime.
              Após cadastrar, mande um áudio para começar!
            </p>
            <div className="space-y-1.5">
              <Label htmlFor="onboarding-phone">Número de WhatsApp</Label>
              <Input
                id="onboarding-phone"
                type="tel"
                placeholder="+55 11 99999-9999"
                value={phone}
                onChange={(e) => { setPhone(e.target.value); setPhoneSaved(false); }}
              />
              {phoneError && <p className="text-xs text-red-600">{phoneError}</p>}
              {phoneSaved && <p className="text-xs text-green-600">Salvo com sucesso!</p>}
            </div>
            <a
              href={SUPPORT_WHATSAPP_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center gap-1.5 text-sm text-green-600 hover:underline"
            >
              <MessageCircle className="w-4 h-4" />
              Falar agora com o ManejoPrime no WhatsApp
            </a>
            <div className="flex items-center justify-between pt-2">
              <button
                onClick={() => setStep(2)}
                className="text-sm text-muted-foreground hover:text-foreground underline"
                type="button"
              >
                Fazer depois →
              </button>
              <Button
                onClick={async () => { await savePhone(); setStep(2); }}
                disabled={phoneSaving || !phone.trim()}
                type="button"
              >
                {phoneSaving ? "Salvando..." : "Salvar e continuar"}
              </Button>
            </div>
          </div>
        )}

        {/* ── Step 2: Team ── */}
        {step === 2 && (
          <div className="space-y-5">
            <p className="text-sm text-muted-foreground">
              Seu veterinário pode registrar vacinas e seu funcionário pode registrar pesagens e ocorrências pelo WhatsApp ou app.
            </p>

            {/* Vet invite */}
            <div className="rounded-lg border p-4 space-y-3">
              <p className="text-sm font-medium">Convidar veterinário</p>
              {vetSent ? (
                <p className="text-sm text-green-600">Convite enviado!</p>
              ) : (
                <>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <Label>E-mail</Label>
                      <Input
                        type="email"
                        placeholder="vet@fazenda.com"
                        value={vetForm.email}
                        onChange={(e) => setVetForm((f) => ({ ...f, email: e.target.value }))}
                      />
                      {vetFieldErrors.email && <p className="text-xs text-red-600">{vetFieldErrors.email}</p>}
                    </div>
                    <div className="space-y-1">
                      <Label>WhatsApp</Label>
                      <Input
                        type="tel"
                        placeholder="+55 11 99999-9999"
                        value={vetForm.phone}
                        onChange={(e) => setVetForm((f) => ({ ...f, phone: e.target.value }))}
                      />
                    </div>
                  </div>
                  {vetError && <p className="text-xs text-red-600">{vetError}</p>}
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!vetForm.email.trim()}
                    onClick={() =>
                      sendInvite(
                        vetForm,
                        () => setVetSent(true),
                        (msg) => setVetError(msg),
                        (fe) => setVetFieldErrors(fe)
                      )
                    }
                    type="button"
                  >
                    Enviar convite
                  </Button>
                </>
              )}
            </div>

            {/* Worker invite */}
            <div className="rounded-lg border p-4 space-y-3">
              <p className="text-sm font-medium">Convidar funcionário</p>
              {workerSent ? (
                <p className="text-sm text-green-600">Convite enviado!</p>
              ) : (
                <>
                  <div className="grid grid-cols-2 gap-3">
                    <div className="space-y-1">
                      <Label>E-mail</Label>
                      <Input
                        type="email"
                        placeholder="func@fazenda.com"
                        value={workerForm.email}
                        onChange={(e) => setWorkerForm((f) => ({ ...f, email: e.target.value }))}
                      />
                      {workerFieldErrors.email && <p className="text-xs text-red-600">{workerFieldErrors.email}</p>}
                    </div>
                    <div className="space-y-1">
                      <Label>WhatsApp</Label>
                      <Input
                        type="tel"
                        placeholder="+55 11 99999-9999"
                        value={workerForm.phone}
                        onChange={(e) => setWorkerForm((f) => ({ ...f, phone: e.target.value }))}
                      />
                    </div>
                  </div>
                  {workerError && <p className="text-xs text-red-600">{workerError}</p>}
                  <Button
                    size="sm"
                    variant="outline"
                    disabled={!workerForm.email.trim()}
                    onClick={() =>
                      sendInvite(
                        workerForm,
                        () => setWorkerSent(true),
                        (msg) => setWorkerError(msg),
                        (fe) => setWorkerFieldErrors(fe)
                      )
                    }
                    type="button"
                  >
                    Enviar convite
                  </Button>
                </>
              )}
            </div>

            <div className="flex items-center justify-between pt-1">
              <button
                onClick={() => setStep(3)}
                className="text-sm text-muted-foreground hover:text-foreground underline"
                type="button"
              >
                Pular esta etapa →
              </button>
              <Button onClick={() => setStep(3)} type="button">Continuar</Button>
            </div>
          </div>
        )}

        {/* ── Step 3: Import ── */}
        {step === 3 && (
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Importe seus lotes, animais e vacinas de uma planilha. Siga a ordem: Lotes → Animais → Vacinas.
            </p>
            <Tabs value={importTab} onValueChange={(v) => setImportTab(v as "lots" | "animals" | "vaccines")}>
              <TabsList className="w-full">
                <TabsTrigger value="lots" className="flex-1">Lotes</TabsTrigger>
                <TabsTrigger value="animals" className="flex-1">Animais</TabsTrigger>
                <TabsTrigger value="vaccines" className="flex-1">Vacinas</TabsTrigger>
              </TabsList>
              <TabsContent value="lots" className="mt-4">
                <ImportSheet
                  dataType="lots"
                  onImported={() => {
                    setHasLotsNow(true);
                    onStatusRefresh();
                  }}
                  onNext={() => setImportTab("animals")}
                />
              </TabsContent>
              <TabsContent value="animals" className="mt-4">
                <ImportSheet
                  dataType="animals"
                  hasLots={hasLotsNow}
                  onImported={() => {
                    setHasAnimalsNow(true);
                    onStatusRefresh();
                  }}
                  onNext={() => setImportTab("vaccines")}
                />
              </TabsContent>
              <TabsContent value="vaccines" className="mt-4">
                <ImportSheet
                  dataType="vaccines"
                  hasAnimals={hasAnimalsNow}
                  onImported={onStatusRefresh}
                  onNext={onDismiss}
                />
              </TabsContent>
            </Tabs>
            <div className="flex justify-end pt-2">
              <Button variant="outline" onClick={onDismiss} type="button">
                Concluir
              </Button>
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="pt-4 border-t mt-4">
          <SupportLinks />
        </div>
      </DialogContent>
    </Dialog>
  );
}
