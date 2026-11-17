import { useEffect, useMemo, useState } from "react";
import {
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from "recharts";
import { Scales } from "@phosphor-icons/react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import { formatAnimalStatus } from "../../lib/formatters";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { DatePickerInput } from "../../components/ui/date-picker";
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

type WeighingListResponse = {
  items: WeighingRecord[];
  total: number;
  limit: number;
  offset: number;
};

type WeighingRecord = {
  id: string;
  animalId: string;
  weightKg: number;
  weighingDate: string;
  type: string;
  source: string;
  recordedBy: string;
  gmdKgPerDay: number | null;
};

type WeighingFormState = {
  weightKg: string;
  weighingDate: string;
  type: string;
};

const DEFAULT_WEIGHING_FORM: WeighingFormState = {
  weightKg: "",
  weighingDate: "",
  type: "routine",
};

export function WeighingsPage() {
  const { fetchProtectedJson, session } = useAuth();
  const permissions = session?.permissions ?? [];
  const canManageWeighings = permissions.includes("weighings.write");
  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [selectedAnimalId, setSelectedAnimalId] = useState("");
  const [animalSearch, setAnimalSearch] = useState("");
  const [weighings, setWeighings] = useState<WeighingListResponse | null>(null);
  const [loadingAnimals, setLoadingAnimals] = useState(false);
  const [loadingWeighings, setLoadingWeighings] = useState(false);
  const [optionsError, setOptionsError] = useState<string | null>(null);
  const [weighingsError, setWeighingsError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [weighingForm, setWeighingForm] = useState<WeighingFormState>(DEFAULT_WEIGHING_FORM);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const visibleAnimals = useMemo(() => {
    const normalizedSearch = animalSearch.trim().toLowerCase();
    if (normalizedSearch.length === 0) return animals;
    return animals.filter((animal) => {
      const searchable = [animal.identifier, animal.name, animal.breed, animal.lotName]
        .filter((value): value is string => value != null)
        .join(" ")
        .toLowerCase();
      return searchable.includes(normalizedSearch);
    });
  }, [animalSearch, animals]);

  useEffect(() => {
    if (!canManageWeighings) return;
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
  }, [canManageWeighings, fetchProtectedJson]);

  useEffect(() => {
    if (!canManageWeighings || visibleAnimals.length === 0) return;
    if (!visibleAnimals.some((animal) => animal.id === selectedAnimalId)) {
      setSelectedAnimalId(visibleAnimals[0].id);
    }
  }, [canManageWeighings, selectedAnimalId, visibleAnimals]);

  useEffect(() => {
    if (!canManageWeighings || selectedAnimalId.length === 0) {
      setWeighings(null);
      return;
    }
    let cancelled = false;

    async function loadWeighings() {
      setLoadingWeighings(true);
      try {
        const nextWeighings = await fetchProtectedJson<WeighingListResponse>(
          `/api/v1/weighings?animalId=${selectedAnimalId}&limit=20&offset=0`
        );
        if (!cancelled) {
          setWeighings(nextWeighings);
          setWeighingsError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setWeighings(null);
          setWeighingsError(describeApiError(error));
        }
      } finally {
        if (!cancelled) setLoadingWeighings(false);
      }
    }

    void loadWeighings();
    return () => { cancelled = true; };
  }, [canManageWeighings, fetchProtectedJson, selectedAnimalId]);

  const selectedAnimal = animals.find((animal) => animal.id === selectedAnimalId) ?? null;

  async function submitWeighing() {
    if (!canManageWeighings || selectedAnimalId.length === 0) return;
    setSubmitting(true);
    setSaveStatus(null);
    setFieldErrors({});
    try {
      const created = await fetchProtectedJson<WeighingRecord>("/api/v1/weighings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          animalId: selectedAnimalId,
          weightKg: weighingForm.weightKg,
          weighingDate: weighingForm.weighingDate,
          type: weighingForm.type,
        }),
      });
      setWeighings((current) => ({
        items: [created, ...(current?.items ?? [])],
        total: (current?.total ?? 0) + 1,
        limit: current?.limit ?? 20,
        offset: 0,
      }));
      setWeighingForm(DEFAULT_WEIGHING_FORM);
      setSaveStatus("Pesagem salva.");
    } catch (error) {
      setFieldErrors(readFieldErrors(error));
      setSaveStatus(describeApiError(error));
    } finally {
      setSubmitting(false);
    }
  }

  if (!canManageWeighings) {
    return (
      <div className="space-y-6">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Pesagens</h1>
        <p className="text-sm text-muted-foreground">
          Apenas usuários com permissão de pesagem podem registrar e consultar o histórico de pesagens.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h1 className="font-heading text-2xl font-bold tracking-tight">Pesagens</h1>

      {/* Animal selector */}
      <div className="flex flex-wrap gap-4 items-end">
        <div className="space-y-1.5 min-w-56">
          <Label htmlFor="animal-search">Buscar animal</Label>
          <Input
            id="animal-search"
            onChange={(e) => setAnimalSearch(e.target.value)}
            placeholder="Identificador, nome, raça ou lote"
            value={animalSearch}
          />
        </div>
        <div className="space-y-1.5 min-w-56">
          <Label htmlFor="animal-select">Animal</Label>
          <select
            aria-label="Animal"
            className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
            disabled={loadingAnimals || visibleAnimals.length === 0}
            id="animal-select"
            onChange={(e) => setSelectedAnimalId(e.target.value)}
            value={selectedAnimalId}
          >
            {visibleAnimals.map((animal) => (
              <option key={animal.id} value={animal.id}>
                {formatAnimalOption(animal)}
              </option>
            ))}
          </select>
        </div>
        {selectedAnimal != null && (
          <span className="text-sm text-muted-foreground pb-1">
            {describeAnimalContext(selectedAnimal)}
          </span>
        )}
      </div>

      {optionsError != null && (
        <p className="text-sm text-destructive">{optionsError}</p>
      )}

      {weighings != null && weighings.items.length >= 2 && (
        <div className="rounded-xl border border-border bg-card p-4">
          <p className="mb-2 text-xs font-medium text-muted-foreground">Evolução de peso e GMD</p>
          <ResponsiveContainer width="100%" height={192}>
            <LineChart
              data={[...weighings.items].reverse().map((w) => ({
                date: w.weighingDate.slice(5).replace("-", "/"),
                Peso: w.weightKg,
                GMD: w.gmdKgPerDay ?? undefined,
              }))}
              margin={{ top: 4, right: 16, left: 0, bottom: 0 }}
            >
              <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="var(--border)" />
              <YAxis yAxisId="left" tick={{ fontSize: 11 }} stroke="var(--border)" unit=" kg" width={52} />
              <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 11 }} stroke="var(--border)" unit=" kg/d" width={56} />
              <Tooltip formatter={(value: number, name: string) => [`${value.toFixed(2)} ${name === "GMD" ? "kg/d" : "kg"}`, name]} />
              <Legend />
              <Line yAxisId="left" type="monotone" dataKey="Peso" stroke="var(--primary)" dot={false} strokeWidth={2} />
              <Line yAxisId="right" type="monotone" dataKey="GMD" stroke="#10b981" dot={false} strokeWidth={2} strokeDasharray="5 3" connectNulls={false} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      )}

      {/* Weighing history table */}
      <div className="overflow-hidden rounded-xl border border-border bg-card">
        {loadingAnimals || loadingWeighings ? (
          <div className="p-4 space-y-2" data-testid="weighings-skeleton">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-3/4" />
          </div>
        ) : weighingsError != null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">{weighingsError}</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Data</TableHead>
                <TableHead>Peso (kg)</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead>Delta</TableHead>
                <TableHead>GMD</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {weighings == null || weighings.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5}>
                    <Empty>
                      <EmptyMedia>
                        <Scales size={80} weight="duotone" className="text-muted-foreground" />
                      </EmptyMedia>
                      <EmptyTitle>Nenhuma pesagem registrada</EmptyTitle>
                      <EmptyDescription>Registre a primeira pesagem para este animal usando o formulário abaixo.</EmptyDescription>
                    </Empty>
                  </TableCell>
                </TableRow>
              ) : (
                weighings.items.map((weighing, index) => {
                  const previousEntry = weighings.items[index + 1] ?? null;
                  const deltaKg = previousEntry == null ? null : weighing.weightKg - previousEntry.weightKg;
                  return (
                    <TableRow key={weighing.id}>
                      <TableCell className="pl-4">{weighing.weighingDate}</TableCell>
                      <TableCell className="font-medium">{formatWeight(weighing.weightKg)}</TableCell>
                      <TableCell>{formatWeighingType(weighing.type)}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {deltaKg == null ? "Pesagem inicial" : `Delta ${formatDelta(deltaKg)}`}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {weighing.gmdKgPerDay == null ? "Sem GMD" : formatGmd(weighing.gmdKgPerDay)}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Register weighing form */}
      <div className="overflow-hidden rounded-xl border border-border bg-card p-6 space-y-4">
        <div>
          <h2 className="font-heading text-lg font-semibold">Registrar pesagem</h2>
          <p className="text-sm text-muted-foreground">
            Registre uma nova medição de peso para o animal selecionado.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="weight-kg">Peso (kg)</Label>
            <Input
              aria-label="Peso (kg)"
              id="weight-kg"
              onChange={(e) => setWeighingForm((f) => ({ ...f, weightKg: e.target.value }))}
              type="number"
              value={weighingForm.weightKg}
            />
            {fieldErrors.weightKg != null && (
              <p className="text-xs text-destructive">{fieldErrors.weightKg}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="weighing-date">Data da pesagem</Label>
            <DatePickerInput
              id="weighing-date"
              onChange={(v) => setWeighingForm((f) => ({ ...f, weighingDate: v }))}
              placeholder="Selecionar data"
              value={weighingForm.weighingDate}
            />
            {fieldErrors.weighingDate != null && (
              <p className="text-xs text-destructive">{fieldErrors.weighingDate}</p>
            )}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="weighing-type">Tipo de pesagem</Label>
            <select
              aria-label="Tipo de pesagem"
              className="flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
              id="weighing-type"
              onChange={(e) => setWeighingForm((f) => ({ ...f, type: e.target.value }))}
              value={weighingForm.type}
            >
              <option value="routine">Rotina</option>
              <option value="entry">Entrada</option>
              <option value="exit">Saída</option>
              <option value="weaning">Desmama</option>
              <option value="slaughter">Abate</option>
            </select>
          </div>
        </div>

        {saveStatus != null && (
          <p className="text-sm text-muted-foreground">{saveStatus}</p>
        )}

        <BillingGatedButton
          disabled={submitting}
          onClick={() => { void submitWeighing(); }}
          type="button"
        >
          {submitting ? "Salvando..." : "Salvar pesagem"}
        </BillingGatedButton>
      </div>
    </div>
  );
}

function describeAnimalContext(animal: AnimalOption) {
  return `Animal ${animal.identifier} · ${animal.name ?? "Sem nome"}`;
}

function formatAnimalOption(animal: AnimalOption) {
  return `${animal.identifier} · ${animal.name ?? "Sem nome"} · ${formatAnimalStatus(animal.status)}`;
}

function formatWeight(weightKg: number) {
  return `${weightKg.toFixed(2)} kg`;
}

function formatDelta(deltaKg: number) {
  const normalizedDelta = deltaKg >= 0 ? `+${deltaKg.toFixed(2)}` : deltaKg.toFixed(2);
  return `${normalizedDelta} kg`;
}

function formatGmd(gmdKgPerDay: number) {
  return `${gmdKgPerDay.toFixed(2)} kg/dia GMD`;
}

function formatWeighingType(type: string) {
  switch (type) {
    case "routine": return "Rotina";
    case "entry": return "Entrada";
    case "exit": return "Saída";
    case "weaning": return "Desmama";
    case "slaughter": return "Abate";
    default: return type;
  }
}

