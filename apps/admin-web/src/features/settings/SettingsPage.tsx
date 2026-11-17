import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { MyFarmsSection } from "./MyFarmsSection";
import { describeApiError, readFieldErrors } from "@/lib/apiErrors";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { InfoTooltip } from "@/components/InfoTooltip";
import { SupportLinks } from "@/components/SupportLinks";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";

type SettingsResponse = {
  farmId: string;
  farmName: string;
  carcassYieldPercent: string;
};

type FormState = {
  farmName: string;
  carcassYieldPercent: string;
};

type ProfileFormState = {
  phone: string;
};

export function SettingsPage() {
  const { fetchProtectedJson, session, refreshBillingStatus } = useAuth();

  const [settings, setSettings] = useState<SettingsResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [form, setForm] = useState<FormState>({ farmName: "", carcassYieldPercent: "" });
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);

  const [profileForm, setProfileForm] = useState<ProfileFormState>({ phone: session?.phone ?? "" });
  const [profileSaving, setProfileSaving] = useState(false);
  const [profileSaveSuccess, setProfileSaveSuccess] = useState(false);
  const [profileSaveError, setProfileSaveError] = useState<string | null>(null);
  const isProfileDirty = profileForm.phone !== (session?.phone ?? "");

  useEffect(() => {
    let cancelled = false;

    async function loadSettings() {
      try {
        const result = await fetchProtectedJson<SettingsResponse>("/api/v1/settings");
        if (!cancelled) {
          setSettings(result);
          setForm({ farmName: result.farmName, carcassYieldPercent: result.carcassYieldPercent });
          setLoadError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setLoadError(describeApiError(error));
        }
      }
    }

    void loadSettings();

    return () => {
      cancelled = true;
    };
  }, [fetchProtectedJson]);

  const isDirty =
    settings != null &&
    (form.farmName !== settings.farmName || form.carcassYieldPercent !== settings.carcassYieldPercent);

  function handleCancel() {
    if (settings == null) return;
    setForm({ farmName: settings.farmName, carcassYieldPercent: settings.carcassYieldPercent });
    setFieldErrors({});
    setSaveError(null);
    setSaveSuccess(false);
  }

  async function handleProfileSave(event: React.FormEvent) {
    event.preventDefault();
    setProfileSaveError(null);
    setProfileSaveSuccess(false);
    setProfileSaving(true);
    try {
      await fetchProtectedJson("/api/v1/profile", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ phone: profileForm.phone.trim() || null })
      });
      setProfileSaveSuccess(true);
    } catch (error) {
      setProfileSaveError(describeApiError(error));
    } finally {
      setProfileSaving(false);
    }
  }

  async function handleSave(event: React.FormEvent) {
    event.preventDefault();
    setFieldErrors({});
    setSaveError(null);
    setSaveSuccess(false);
    setSaving(true);
    try {
      const updated = await fetchProtectedJson<SettingsResponse>("/api/v1/settings", {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          farmName: form.farmName.trim(),
          carcassYieldPercent: form.carcassYieldPercent.trim()
        })
      });
      setSettings(updated);
      setForm({ farmName: updated.farmName, carcassYieldPercent: updated.carcassYieldPercent });
      setSaveSuccess(true);
      window.dispatchEvent(new Event("farms-updated"));
      void refreshBillingStatus();
    } catch (error) {
      setFieldErrors(readFieldErrors(error));
      setSaveError(describeApiError(error));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="space-y-6">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Configurações</h1>

        {loadError != null ? (
          <Card>
            <CardHeader>
              <CardTitle>Configurações indisponíveis</CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-destructive">{loadError}</p>
            </CardContent>
          </Card>
        ) : settings == null ? (
          <div data-testid="settings-skeleton" className="space-y-4">
            <Skeleton className="h-10 w-48 rounded-md" />
            <Skeleton className="h-64 rounded-xl" />
          </div>
        ) : (
          <Card className="max-w-lg">
            <CardHeader>
              <CardTitle>{settings.farmName}</CardTitle>
              <p className="text-xs text-muted-foreground">Configuração da fazenda</p>
            </CardHeader>
            <CardContent>
              <form className="space-y-5" onSubmit={(e) => void handleSave(e)}>
                <div className="space-y-2">
                  <Label htmlFor="farmName">Nome da fazenda</Label>
                  <Input
                    id="farmName"
                    onChange={(e) => {
                      setForm((f) => ({ ...f, farmName: e.target.value }));
                      setSaveSuccess(false);
                    }}
                    type="text"
                    value={form.farmName}
                  />
                  {fieldErrors["farmName"] ? (
                    <p className="text-xs text-destructive">{fieldErrors["farmName"]}</p>
                  ) : null}
                </div>

                <div className="space-y-2">
                  <Label className="flex items-center gap-1.5" htmlFor="carcassYieldPercent">
                    Rendimento de carcaça (%)
                    <InfoTooltip text="% do peso vivo que vira carne após o abate. Ex: boi de 500 kg com 55% rende 275 kg de carcaça. Valor médio: 50–60%" />
                  </Label>
                  <Input
                    id="carcassYieldPercent"
                    onChange={(e) => {
                      setForm((f) => ({ ...f, carcassYieldPercent: e.target.value }));
                      setSaveSuccess(false);
                    }}
                    type="text"
                    value={form.carcassYieldPercent}
                  />
                  {fieldErrors["carcassYieldPercent"] ? (
                    <p className="text-xs text-destructive">{fieldErrors["carcassYieldPercent"]}</p>
                  ) : null}
                </div>

                {saveError != null ? <p className="text-sm text-destructive">{saveError}</p> : null}
                {saveSuccess ? <p className="text-sm text-green-600">Configurações salvas.</p> : null}

                <div className="flex gap-2 justify-end">
                  <Button disabled={!isDirty} onClick={handleCancel} type="button" variant="outline">
                    Cancelar
                  </Button>
                  <Button disabled={!isDirty || saving} type="submit">
                    {saving ? "Salvando..." : "Salvar"}
                  </Button>
                </div>
              </form>
            </CardContent>
          </Card>
        )}

        <Card className="max-w-lg">
          <CardHeader>
            <CardTitle>Meu perfil</CardTitle>
            <p className="text-xs text-muted-foreground">Número de WhatsApp para entrada de voz</p>
          </CardHeader>
          <CardContent>
            <form className="space-y-5" onSubmit={(e) => void handleProfileSave(e)}>
              <div className="space-y-2">
                <Label className="flex items-center gap-1.5" htmlFor="profilePhone">
                  Telefone
                  <InfoTooltip text="Número do WhatsApp com DDD. Ex: 11912345678. Usado para receber alertas e registrar dados por voz." />
                </Label>
                <Input
                  id="profilePhone"
                  onChange={(e) => {
                    setProfileForm({ phone: e.target.value });
                    setProfileSaveSuccess(false);
                  }}
                  placeholder="Ex: +5511999990001"
                  type="text"
                  value={profileForm.phone}
                />
              </div>

              {profileSaveError != null ? <p className="text-sm text-destructive">{profileSaveError}</p> : null}
              {profileSaveSuccess ? (
                <p className="text-sm text-green-600">Telefone atualizado. Mensagem de boas-vindas enviada se for um número novo.</p>
              ) : null}

              <div className="flex gap-2 justify-end">
                <Button
                  disabled={!isProfileDirty}
                  onClick={() => {
                    setProfileForm({ phone: session?.phone ?? "" });
                    setProfileSaveSuccess(false);
                    setProfileSaveError(null);
                  }}
                  type="button"
                  variant="outline"
                >
                  Redefinir
                </Button>
                <Button disabled={!isProfileDirty || profileSaving} type="submit">
                  {profileSaving ? "Salvando..." : "Salvar telefone"}
                </Button>
              </div>
            </form>
          </CardContent>
        </Card>
        <MyFarmsSection />
        <SupportLinks />
    </div>
  );
}

