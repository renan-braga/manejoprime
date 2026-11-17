import { useEffect, useState } from "react";
import { Pill } from "@phosphor-icons/react";
import { Trash2 } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import { capitalize } from "../../lib/formatters";
import { Badge } from "../../components/ui/badge";
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

type MedicationListResponse = {
  items: MedicationRecord[];
  total: number;
  limit: number;
  offset: number;
};

type MedicationRecord = {
  id: string;
  animalId: string;
  occurrenceId: string | null;
  medicationName: string;
  activeIngredient: string | null;
  reason: string | null;
  dosage: string;
  withdrawalPeriodDays: number;
  applicationDate: string;
  releaseDate: string;
  inWithdrawalPeriod: boolean;
  cost: number | null;
  source: string;
  recordedBy: string;
};

type MedicationFormState = {
  occurrenceId: string;
  medicationName: string;
  activeIngredient: string;
  reason: string;
  dosage: string;
  withdrawalPeriodDays: string;
  applicationDate: string;
  cost: string;
};

const DEFAULT_MEDICATION_FORM: MedicationFormState = {
  occurrenceId: "",
  medicationName: "",
  activeIngredient: "",
  reason: "",
  dosage: "",
  withdrawalPeriodDays: "0",
  applicationDate: "",
  cost: "",
};

export function MedicationsPage() {
  const { fetchProtectedJson } = useAuth();
  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [selectedAnimalId, setSelectedAnimalId] = useState("");
  const [medications, setMedications] = useState<MedicationListResponse | null>(null);
  const [loadingAnimals, setLoadingAnimals] = useState(false);
  const [loadingMedications, setLoadingMedications] = useState(false);
  const [optionsError, setOptionsError] = useState<string | null>(null);
  const [medicationsError, setMedicationsError] = useState<string | null>(null);
  const [medicationFieldErrors, setMedicationFieldErrors] = useState<Record<string, string>>({});
  const [medicationForm, setMedicationForm] = useState<MedicationFormState>(DEFAULT_MEDICATION_FORM);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [deleteTarget, setDeleteTarget] = useState<{ id: string; label: string } | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function loadAnimals() {
      setLoadingAnimals(true);
      try {
        const animalResponse = await fetchProtectedJson<AnimalListResponse>("/api/v1/animals?limit=100&offset=0");
        if (cancelled) return;
        setAnimals(animalResponse.items);
        setSelectedAnimalId((current) => current || animalResponse.items[0]?.id || "");
        setOptionsError(null);
      } catch (error) {
        if (!cancelled) setOptionsError(describeApiError(error));
      } finally {
        if (!cancelled) setLoadingAnimals(false);
      }
    }

    void loadAnimals();
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  useEffect(() => {
    if (selectedAnimalId.length === 0) {
      setMedications(null);
      return;
    }
    let cancelled = false;

    async function loadMedications() {
      setLoadingMedications(true);
      try {
        const nextMedications = await fetchProtectedJson<MedicationListResponse>(
          `/api/v1/medications?animalId=${selectedAnimalId}&limit=20&offset=0`
        );
        if (!cancelled) {
          setMedications(nextMedications);
          setMedicationsError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setMedications(null);
          setMedicationsError(describeApiError(error));
        }
      } finally {
        if (!cancelled) setLoadingMedications(false);
      }
    }

    void loadMedications();
    return () => { cancelled = true; };
  }, [fetchProtectedJson, selectedAnimalId]);

  const selectedAnimal = animals.find((a) => a.id === selectedAnimalId) ?? null;
  const activeCarencia = medications?.items.filter((m) => calculateCarenciaDaysRemaining(m) > 0).length ?? 0;
  const expiredCarencia = medications?.items.filter((m) => calculateCarenciaDaysRemaining(m) <= 0).length ?? 0;

  async function submitMedication() {
    if (selectedAnimalId.length === 0) return;
    setSubmitting(true);
    setSaveStatus(null);
    setMedicationFieldErrors({});
    const payload = {
      animalId: selectedAnimalId,
      occurrenceId: medicationForm.occurrenceId || undefined,
      medicationName: medicationForm.medicationName,
      activeIngredient: medicationForm.activeIngredient || undefined,
      reason: medicationForm.reason || undefined,
      dosage: medicationForm.dosage,
      withdrawalPeriodDays: Number(medicationForm.withdrawalPeriodDays),
      applicationDate: medicationForm.applicationDate,
      cost: medicationForm.cost || undefined,
    };
    try {
      const created = await fetchProtectedJson<MedicationRecord>("/api/v1/medications", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      setMedications((current) => ({
        items: [created, ...(current?.items ?? [])],
        total: (current?.total ?? 0) + 1,
        limit: current?.limit ?? 20,
        offset: 0,
      }));
      setMedicationForm(DEFAULT_MEDICATION_FORM);
      setSaveStatus("Medicamento salvo.");
    } catch (error) {
      setMedicationFieldErrors(readFieldErrors(error));
      setSaveStatus(describeApiError(error));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteMedication() {
    if (deleteTarget == null) return;
    setDeleting(true);
    try {
      await fetchProtectedJson(`/api/v1/medications/${deleteTarget.id}`, { method: "DELETE" });
      setMedications((current) =>
        current == null
          ? null
          : { ...current, items: current.items.filter((m) => m.id !== deleteTarget.id), total: current.total - 1 }
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
            <Button disabled={deleting} onClick={() => { void handleDeleteMedication(); }} type="button" variant="destructive">
              {deleting ? "Excluindo..." : "Excluir"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">Medicamentos</h1>
        <p className="text-sm text-muted-foreground">
          Acompanhe tratamentos, ocorrências vinculadas e períodos de carência por animal.
        </p>
      </div>

      {optionsError != null && <p className="text-sm text-destructive">{optionsError}</p>}

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Registros visíveis</p>
          <p className="mt-1 text-2xl font-semibold">{medications?.total ?? 0}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Carência ativa</p>
          <p className="mt-1 text-2xl font-semibold">{activeCarencia}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Carência vencida</p>
          <p className="mt-1 text-2xl font-semibold">{expiredCarencia}</p>
        </div>
      </div>

      <div className="rounded-lg border border-border bg-card p-6 space-y-4">
        <div>
          <h2 className="font-heading text-lg font-semibold">Registrar medicamento</h2>
          <p className="text-sm text-muted-foreground">Registre os detalhes do tratamento e o período de carência.</p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="med-animal">Animal</Label>
            <select
              aria-label="Animal"
              className={selectClass}
              id="med-animal"
              onChange={(e) => setSelectedAnimalId(e.target.value)}
              value={selectedAnimalId}
            >
              {animals.map((a) => (
                <option key={a.id} value={a.id}>{formatAnimalOption(a)}</option>
              ))}
            </select>
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="occurrence-id">ID da ocorrência</Label>
            <Input
              aria-label="Occurrence ID"
              id="occurrence-id"
              onChange={(e) => setMedicationForm((f) => ({ ...f, occurrenceId: e.target.value }))}
              value={medicationForm.occurrenceId}
            />
            {medicationFieldErrors.occurrenceId && <p className="text-xs text-destructive">{medicationFieldErrors.occurrenceId}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="medication-name">Nome do medicamento</Label>
            <Input
              aria-label="Medication name"
              id="medication-name"
              onChange={(e) => setMedicationForm((f) => ({ ...f, medicationName: e.target.value }))}
              value={medicationForm.medicationName}
            />
            {medicationFieldErrors.medicationName && <p className="text-xs text-destructive">{medicationFieldErrors.medicationName}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="active-ingredient">Princípio ativo</Label>
            <Input
              aria-label="Active ingredient"
              id="active-ingredient"
              onChange={(e) => setMedicationForm((f) => ({ ...f, activeIngredient: e.target.value }))}
              value={medicationForm.activeIngredient}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="reason">Motivo</Label>
            <Input
              aria-label="Reason"
              id="reason"
              onChange={(e) => setMedicationForm((f) => ({ ...f, reason: e.target.value }))}
              value={medicationForm.reason}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="dosage">Dosagem</Label>
            <Input
              aria-label="Dosage"
              id="dosage"
              onChange={(e) => setMedicationForm((f) => ({ ...f, dosage: e.target.value }))}
              value={medicationForm.dosage}
            />
            {medicationFieldErrors.dosage && <p className="text-xs text-destructive">{medicationFieldErrors.dosage}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="withdrawal-days">Período de carência (dias)</Label>
            <Input
              aria-label="Withdrawal period (days)"
              id="withdrawal-days"
              onChange={(e) => setMedicationForm((f) => ({ ...f, withdrawalPeriodDays: e.target.value }))}
              type="number"
              value={medicationForm.withdrawalPeriodDays}
            />
            {medicationFieldErrors.withdrawalPeriodDays && <p className="text-xs text-destructive">{medicationFieldErrors.withdrawalPeriodDays}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="med-app-date">Data de aplicação</Label>
            <DatePickerInput
              id="med-app-date"
              onChange={(v) => setMedicationForm((f) => ({ ...f, applicationDate: v }))}
              placeholder="Selecionar data"
              value={medicationForm.applicationDate}
            />
            {medicationFieldErrors.applicationDate && <p className="text-xs text-destructive">{medicationFieldErrors.applicationDate}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="cost">Cost</Label>
            <Input
              aria-label="Cost"
              id="cost"
              onChange={(e) => setMedicationForm((f) => ({ ...f, cost: e.target.value }))}
              type="number"
              value={medicationForm.cost}
            />
          </div>
        </div>

        {saveStatus != null && <p className="text-sm text-muted-foreground">{saveStatus}</p>}
        <BillingGatedButton
          disabled={submitting || loadingAnimals}
          onClick={() => { void submitMedication(); }}
          type="button"
        >
          Salvar medicamento
        </BillingGatedButton>
      </div>

      <div>
        <h2 className="font-heading text-lg font-semibold mb-3">
          Histórico de medicamentos
          {selectedAnimal != null && (
            <span className="ml-2 text-sm font-normal text-muted-foreground">
              {describeAnimalContext(selectedAnimal)}
            </span>
          )}
        </h2>
        <div className="overflow-hidden rounded-lg border border-border bg-card">
          {loadingMedications || loadingAnimals ? (
            <div className="p-4 space-y-2" data-testid="medications-skeleton">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-3/4" />
            </div>
          ) : medicationsError != null ? (
            <div className="p-8 text-center text-sm text-muted-foreground">{medicationsError}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Nome</TableHead>
                  <TableHead>Dosagem e Motivo</TableHead>
                  <TableHead>Aplicado</TableHead>
                  <TableHead>Carência</TableHead>
                  <TableHead>Ocorrência</TableHead>
                  <TableHead />
                </TableRow>
              </TableHeader>
              <TableBody>
                {medications == null || medications.items.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Empty>
                        <EmptyMedia>
                          <Pill size={80} weight="duotone" className="text-muted-foreground" />
                        </EmptyMedia>
                        <EmptyTitle>Nenhum medicamento registrado</EmptyTitle>
                        <EmptyDescription>Registre o primeiro medicamento aplicado a este animal.</EmptyDescription>
                      </Empty>
                    </TableCell>
                  </TableRow>
                ) : (
                  medications.items.map((m) => {
                    const carencia = describeCarencia(m);
                    return (
                      <TableRow key={m.id}>
                        <TableCell className="pl-4 font-medium">{m.medicationName}</TableCell>
                        <TableCell>{m.dosage} · {m.reason ?? "Motivo não informado"}</TableCell>
                        <TableCell>{m.applicationDate}</TableCell>
                        <TableCell>
                          <div className="flex flex-col gap-1">
                            <Badge className={carencia.expired ? "border-red-200 bg-red-100 text-red-700" : "border-emerald-200 bg-emerald-100 text-emerald-700"} variant="outline">
                              {carencia.label}
                            </Badge>
                            <span className="text-xs text-muted-foreground">Carência até {carencia.releaseDate}</span>
                          </div>
                        </TableCell>
                        <TableCell className="text-muted-foreground">
                          {m.occurrenceId != null ? `Ocorrência ${m.occurrenceId}` : "Sem ocorrência vinculada"}
                        </TableCell>
                        <TableCell>
                          <Button
                            aria-label="Excluir medicamento"
                            onClick={() => setDeleteTarget({ id: m.id, label: m.medicationName })}
                            size="icon"
                            type="button"
                            variant="ghost"
                          >
                            <Trash2 className="h-4 w-4 text-destructive" />
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })
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

function formatAnimalOption(animal: AnimalOption) {
  return `${animal.identifier} · ${animal.name ?? "Sem nome"} · ${capitalize(animal.status)}`;
}

function describeCarencia(medication: MedicationRecord) {
  const daysRemaining = calculateCarenciaDaysRemaining(medication);
  const releaseDate = formatDate(addDays(parseDateOnly(medication.applicationDate), medication.withdrawalPeriodDays));
  if (daysRemaining > 0) {
    return { expired: false, label: `${daysRemaining} ${daysRemaining === 1 ? "dia" : "dias"} restante${daysRemaining === 1 ? "" : "s"}`, releaseDate };
  }
  const overdueDays = Math.abs(daysRemaining);
  if (overdueDays === 0) return { expired: true, label: "Venceu hoje", releaseDate };
  return { expired: true, label: `Venceu há ${overdueDays} ${overdueDays === 1 ? "dia" : "dias"}`, releaseDate };
}

function calculateCarenciaDaysRemaining(medication: MedicationRecord) {
  const releaseDate = addDays(parseDateOnly(medication.applicationDate), medication.withdrawalPeriodDays);
  const today = startOfUtcDay(new Date());
  return Math.ceil((releaseDate.getTime() - today.getTime()) / 86_400_000);
}

function parseDateOnly(value: string) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(Date.UTC(year, month - 1, day));
}

function addDays(date: Date, days: number) {
  const nextDate = new Date(date);
  nextDate.setUTCDate(nextDate.getUTCDate() + days);
  return nextDate;
}

function startOfUtcDay(date: Date) {
  return new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
}

function formatDate(date: Date) {
  const year = date.getUTCFullYear();
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

