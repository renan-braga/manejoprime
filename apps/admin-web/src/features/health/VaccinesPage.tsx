import { useEffect, useState } from "react";
import { Syringe } from "@phosphor-icons/react";
import { Trash2, Upload } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { ImportSheet } from "../../features/onboarding/ImportSheet";
import { InfoTooltip } from "../../components/InfoTooltip";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import { capitalize } from "../../lib/formatters";
import { Button } from "../../components/ui/button";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { DatePickerInput } from "../../components/ui/date-picker";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../../components/ui/dialog";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Skeleton } from "../../components/ui/skeleton";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { Empty, EmptyMedia, EmptyTitle, EmptyDescription } from "../../components/ui/empty";

type AnimalListResponse = {
  items: AnimalOption[];
  total: number;
  limit: number;
  offset: number;
};

type AnimalOption = {
  id: string;
  identifier: string;
  name: string | null;
  sex: string;
  breed: string | null;
  status: string;
  lotId: string | null;
  lotName: string | null;
};

type LotListResponse = {
  items: LotOption[];
  total: number;
  limit: number;
  offset: number;
};

type LotOption = {
  id: string;
  name: string;
  purpose: string;
  pastureArea: string | null;
  animalsCount: number;
};

type VaccineListResponse = {
  items: VaccineRecord[];
  total: number;
  limit: number;
  offset: number;
};

type VaccineRecord = {
  id: string;
  animalId: string | null;
  lotId: string | null;
  vaccineType: string;
  vaccineBatch: string | null;
  dose: string;
  applicationDate: string;
  nextDoseDate: string | null;
  volumeMl: number | null;
  source: string;
  recordedBy: string;
};

type VaccineTarget = "animal" | "lot";

type VaccineFormState = {
  vaccineType: string;
  vaccineBatch: string;
  dose: string;
  applicationDate: string;
  nextDoseDate: string;
  volumeMl: string;
};

const DEFAULT_VACCINE_FORM: VaccineFormState = {
  vaccineType: "",
  vaccineBatch: "",
  dose: "primeira",
  applicationDate: "",
  nextDoseDate: "",
  volumeMl: "",
};

export function VaccinesPage() {
  const { fetchProtectedJson, session } = useAuth();
  const canWrite = session?.permissions.includes("vaccines.write") ?? false;
  const [vaccineTarget, setVaccineTarget] = useState<VaccineTarget>("animal");
  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [lots, setLots] = useState<LotOption[]>([]);
  const [selectedAnimalId, setSelectedAnimalId] = useState("");
  const [selectedLotId, setSelectedLotId] = useState("");
  const [vaccines, setVaccines] = useState<VaccineListResponse | null>(null);
  const [loadingOptions, setLoadingOptions] = useState(false);
  const [loadingVaccines, setLoadingVaccines] = useState(false);
  const [optionsError, setOptionsError] = useState<string | null>(null);
  const [vaccinesError, setVaccinesError] = useState<string | null>(null);
  const [vaccineFieldErrors, setVaccineFieldErrors] = useState<Record<string, string>>({});
  const [importOpen, setImportOpen] = useState(false);
  const [refreshVaccinesKey, setRefreshVaccinesKey] = useState(0);
  const [vaccineForm, setVaccineForm] = useState<VaccineFormState>(DEFAULT_VACCINE_FORM);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; label: string } | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadOptions() {
      setLoadingOptions(true);
      try {
        const [animalResponse, lotResponse] = await Promise.all([
          fetchProtectedJson<AnimalListResponse>("/api/v1/animals?limit=100&offset=0"),
          fetchProtectedJson<LotListResponse>("/api/v1/lots?limit=100&offset=0"),
        ]);
        if (cancelled) return;
        setAnimals(animalResponse.items);
        setLots(lotResponse.items);
        setSelectedAnimalId((current) => current || animalResponse.items[0]?.id || "");
        setSelectedLotId((current) => current || lotResponse.items[0]?.id || "");
        setOptionsError(null);
      } catch (error) {
        if (!cancelled) setOptionsError(describeApiError(error));
      } finally {
        if (!cancelled) setLoadingOptions(false);
      }
    }

    void loadOptions();
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  useEffect(() => {
    const targetId = vaccineTarget === "animal" ? selectedAnimalId : selectedLotId;
    if (targetId.length === 0) {
      setVaccines(null);
      return;
    }
    let cancelled = false;

    async function loadVaccines() {
      setLoadingVaccines(true);
      try {
        const path =
          vaccineTarget === "animal"
            ? `/api/v1/vaccines?animalId=${selectedAnimalId}&limit=20&offset=0`
            : `/api/v1/vaccines?lotId=${selectedLotId}&limit=20&offset=0`;
        const nextVaccines = await fetchProtectedJson<VaccineListResponse>(path);
        if (!cancelled) {
          setVaccines(nextVaccines);
          setVaccinesError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setVaccines(null);
          setVaccinesError(describeApiError(error));
        }
      } finally {
        if (!cancelled) setLoadingVaccines(false);
      }
    }

    void loadVaccines();
    return () => { cancelled = true; };
  }, [fetchProtectedJson, selectedAnimalId, selectedLotId, vaccineTarget, refreshVaccinesKey]);

  const selectedAnimal = animals.find((a) => a.id === selectedAnimalId) ?? null;
  const selectedLot = lots.find((l) => l.id === selectedLotId) ?? null;
  const upcomingDoses = vaccines?.items.filter((v) => v.nextDoseDate != null).length ?? 0;
  const appRecords = vaccines?.items.filter((v) => v.source === "app").length ?? 0;

  async function submitVaccine() {
    setSubmitting(true);
    setSaveStatus(null);
    setVaccineFieldErrors({});
    const payload = {
      animalId: vaccineTarget === "animal" ? selectedAnimalId : undefined,
      lotId: vaccineTarget === "lot" ? selectedLotId : undefined,
      vaccineType: vaccineForm.vaccineType,
      vaccineBatch: vaccineForm.vaccineBatch || undefined,
      dose: vaccineForm.dose,
      applicationDate: vaccineForm.applicationDate,
      nextDoseDate: vaccineForm.nextDoseDate || undefined,
      volumeMl: vaccineForm.volumeMl || undefined,
    };
    try {
      const created = await fetchProtectedJson<VaccineRecord>("/api/v1/vaccines", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      setVaccines((current) => ({
        items: [created, ...(current?.items ?? [])],
        total: (current?.total ?? 0) + 1,
        limit: current?.limit ?? 20,
        offset: 0,
      }));
      setVaccineForm(DEFAULT_VACCINE_FORM);
      setSaveStatus("Vacina salva.");
    } catch (error) {
      setVaccineFieldErrors(readFieldErrors(error));
      setSaveStatus(describeApiError(error));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteVaccine() {
    if (deleteTarget == null) return;
    setDeleting(true);
    try {
      await fetchProtectedJson(`/api/v1/vaccines/${deleteTarget.id}`, { method: "DELETE" });
      setVaccines((current) =>
        current == null
          ? null
          : { ...current, items: current.items.filter((v) => v.id !== deleteTarget.id), total: current.total - 1 }
      );
      setDeleteTarget(null);
    } catch (error) {
      setSaveStatus(describeApiError(error));
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  }

  const selectClass =
    "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

  return (
    <div className="space-y-6">
      <Dialog onOpenChange={(open) => { if (!open) setDeleteTarget(null); }} open={deleteTarget != null}>
        <DialogContent className="max-w-sm">
          <DialogHeader><DialogTitle>Confirmar exclusão</DialogTitle></DialogHeader>
          <p className="text-sm text-muted-foreground">
            Tem certeza que deseja excluir o registro de <span className="font-medium text-foreground">{deleteTarget?.label}</span>? Esta ação não pode ser desfeita.
          </p>
          <DialogFooter>
            <Button onClick={() => setDeleteTarget(null)} type="button" variant="outline">Cancelar</Button>
            <Button disabled={deleting} onClick={() => { void handleDeleteVaccine(); }} type="button" variant="destructive">
              {deleting ? "Excluindo..." : "Excluir"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <Dialog onOpenChange={setImportOpen} open={importOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Importar vacinas via planilha</DialogTitle>
          </DialogHeader>
          <ImportSheet
            dataType="vaccines"
            onImported={() => {
              setImportOpen(false);
              setRefreshVaccinesKey((k) => k + 1);
            }}
          />
        </DialogContent>
      </Dialog>

      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Vacinas</h1>
          <p className="text-sm text-muted-foreground">
            Registre doses e consulte o histórico de vacinas por animal ou lote.
          </p>
        </div>
        <div className="flex items-center gap-2">
          {canWrite ? (
            <BillingGatedButton onClick={() => setImportOpen(true)} size="sm" type="button" variant="outline">
              <Upload className="h-4 w-4" />
              Importar planilha
            </BillingGatedButton>
          ) : null}
          <div className="flex rounded-md border border-border overflow-hidden" role="group" aria-label="Vaccine target">
          <button
            className={`px-4 py-1.5 text-sm transition-colors ${vaccineTarget === "animal" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
            onClick={() => setVaccineTarget("animal")}
            type="button"
          >
            Animal
          </button>
          <button
            className={`px-4 py-1.5 text-sm transition-colors ${vaccineTarget === "lot" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
            onClick={() => setVaccineTarget("lot")}
            type="button"
          >
            Lote
          </button>
          </div>
        </div>
      </div>

      {optionsError != null && <p className="text-sm text-destructive">{optionsError}</p>}

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Registros visíveis</p>
          <p className="mt-1 text-2xl font-semibold">{vaccines?.total ?? 0}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Próximas doses</p>
          <p className="mt-1 text-2xl font-semibold">{upcomingDoses}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Registros do app</p>
          <p className="mt-1 text-2xl font-semibold">{appRecords}</p>
        </div>
      </div>

      <div className="rounded-lg border border-border bg-card p-6 space-y-4">
        <div>
          <h2 className="font-heading text-lg font-semibold">Registrar vacina</h2>
          <p className="text-sm text-muted-foreground">Escolha o alvo e registre a próxima dose.</p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {vaccineTarget === "animal" ? (
            <div className="space-y-1.5">
              <Label htmlFor="vaccine-animal">Animal</Label>
              <select
                aria-label="Animal"
                className={selectClass}
                id="vaccine-animal"
                onChange={(e) => setSelectedAnimalId(e.target.value)}
                value={selectedAnimalId}
              >
                {animals.map((a) => (
                  <option key={a.id} value={a.id}>{formatAnimalOption(a)}</option>
                ))}
              </select>
            </div>
          ) : (
            <div className="space-y-1.5">
              <Label htmlFor="vaccine-lot">Lote</Label>
              <select
                aria-label="Lote"
                className={selectClass}
                id="vaccine-lot"
                onChange={(e) => setSelectedLotId(e.target.value)}
                value={selectedLotId}
              >
                {lots.map((l) => (
                  <option key={l.id} value={l.id}>{l.name} · {capitalize(l.purpose)}</option>
                ))}
              </select>
            </div>
          )}

          <div className="space-y-1.5">
            <Label htmlFor="vaccine-type">Tipo de vacina</Label>
            <Input
              aria-label="Vaccine type"
              id="vaccine-type"
              onChange={(e) => setVaccineForm((f) => ({ ...f, vaccineType: e.target.value }))}
              value={vaccineForm.vaccineType}
            />
            {vaccineFieldErrors.vaccineType && <p className="text-xs text-destructive">{vaccineFieldErrors.vaccineType}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="vaccine-batch">
              Lote da vacina
              <InfoTooltip text="Número do lote impresso no frasco da vacina — não é o lote de animais" />
            </Label>
            <Input
              aria-label="Vaccine batch"
              id="vaccine-batch"
              onChange={(e) => setVaccineForm((f) => ({ ...f, vaccineBatch: e.target.value }))}
              value={vaccineForm.vaccineBatch}
            />
            {vaccineFieldErrors.vaccineBatch && <p className="text-xs text-destructive">{vaccineFieldErrors.vaccineBatch}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="vaccine-dose">
              Dose da vacina
              <InfoTooltip text="Primeira: 1ª aplicação · Segunda: 2ª dose · Reforço: dose extra sem prazo fixo · Anual: repetição todo ano" />
            </Label>
            <select
              aria-label="Vaccine dose"
              className={selectClass}
              id="vaccine-dose"
              onChange={(e) => setVaccineForm((f) => ({ ...f, dose: e.target.value }))}
              value={vaccineForm.dose}
            >
              <option value="primeira">Primeira</option>
              <option value="segunda">Segunda</option>
              <option value="reforco">Reforco</option>
              <option value="anual">Anual</option>
            </select>
            {vaccineFieldErrors.dose && <p className="text-xs text-destructive">{vaccineFieldErrors.dose}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="vaccine-app-date">Data de aplicação</Label>
            <DatePickerInput
              id="vaccine-app-date"
              onChange={(v) => setVaccineForm((f) => ({ ...f, applicationDate: v }))}
              placeholder="Selecionar data"
              value={vaccineForm.applicationDate}
            />
            {vaccineFieldErrors.applicationDate && <p className="text-xs text-destructive">{vaccineFieldErrors.applicationDate}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="next-dose-date">Data da próxima dose</Label>
            <DatePickerInput
              id="next-dose-date"
              onChange={(v) => setVaccineForm((f) => ({ ...f, nextDoseDate: v }))}
              placeholder="Selecionar data"
              value={vaccineForm.nextDoseDate}
            />
            {vaccineFieldErrors.nextDoseDate && <p className="text-xs text-destructive">{vaccineFieldErrors.nextDoseDate}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="volume-ml">
              Volume (ml)
              <InfoTooltip text="Quantidade de vacina aplicada em mililitros (ml). Verifique a bula do produto." />
            </Label>
            <Input
              aria-label="Volume (ml)"
              id="volume-ml"
              onChange={(e) => setVaccineForm((f) => ({ ...f, volumeMl: e.target.value }))}
              type="number"
              value={vaccineForm.volumeMl}
            />
            {vaccineFieldErrors.volumeMl && <p className="text-xs text-destructive">{vaccineFieldErrors.volumeMl}</p>}
          </div>
        </div>

        {saveStatus != null && <p className="text-sm text-muted-foreground">{saveStatus}</p>}
        <BillingGatedButton
          disabled={submitting || loadingOptions}
          onClick={() => { void submitVaccine(); }}
          type="button"
        >
          Salvar vacina
        </BillingGatedButton>
      </div>

      <div>
        <h2 className="font-heading text-lg font-semibold mb-3">
          Histórico de vacinas
          {selectedAnimal != null && vaccineTarget === "animal" && (
            <span className="ml-2 text-sm font-normal text-muted-foreground">
              {describeAnimalContext(selectedAnimal)}
            </span>
          )}
          {selectedLot != null && vaccineTarget === "lot" && (
            <span className="ml-2 text-sm font-normal text-muted-foreground">
              {describeLotContext(selectedLot)}
            </span>
          )}
        </h2>
        <div className="overflow-hidden rounded-lg border border-border bg-card">
          {loadingVaccines || loadingOptions ? (
            <div className="p-4 space-y-2" data-testid="vaccines-skeleton">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-3/4" />
            </div>
          ) : vaccinesError != null ? (
            <div className="p-8 text-center text-sm text-muted-foreground">{vaccinesError}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Tipo</TableHead>
                  <TableHead>Dose</TableHead>
                  <TableHead>Data</TableHead>
                  <TableHead>Próxima dose</TableHead>
                  <TableHead>Lote</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {vaccines == null || vaccines.items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Empty>
                        <EmptyMedia>
                          <Syringe size={80} weight="duotone" className="text-muted-foreground" />
                        </EmptyMedia>
                        <EmptyTitle>Nenhuma vacina registrada</EmptyTitle>
                        <EmptyDescription>Registre a primeira dose de vacina para este animal ou lote.</EmptyDescription>
                      </Empty>
                    </TableCell>
                  </TableRow>
                ) : (
                  vaccines.items.map((v) => (
                    <TableRow key={v.id}>
                      <TableCell className="pl-4 font-medium">{v.vaccineType}</TableCell>
                      <TableCell>{formatDose(v.dose)}</TableCell>
                      <TableCell>Aplicado em {v.applicationDate}</TableCell>
                      <TableCell>{describeNextDose(v.nextDoseDate)}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {v.vaccineBatch != null ? `Lote ${v.vaccineBatch}` : "Lote não informado"}
                      </TableCell>
                      <TableCell>
                        <Button
                          aria-label="Excluir vacina"
                          onClick={() => setDeleteTarget({ id: v.id, label: v.vaccineType })}
                          size="icon"
                          type="button"
                          variant="ghost"
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          )}
        </div>
      </div>
    </div>
  );
}

function describeAnimalContext(animal: AnimalOption | null) {
  if (animal == null) return "Contexto do animal indisponível";
  return `Animal ${animal.identifier} · ${animal.name ?? "Sem nome"}`;
}

function describeLotContext(lot: LotOption | null) {
  if (lot == null) return "Contexto do lote indisponível";
  return `Lote ${lot.name} · ${capitalize(lot.purpose)}`;
}

function describeNextDose(nextDoseDate: string | null) {
  if (nextDoseDate == null) return "Próxima dose não agendada";
  return `Próxima dose em ${nextDoseDate}`;
}

function formatAnimalOption(animal: AnimalOption) {
  return `${animal.identifier} · ${animal.name ?? "Sem nome"} · ${capitalize(animal.status)}`;
}

function formatDose(value: string) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

