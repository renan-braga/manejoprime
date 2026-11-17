import { useDeferredValue, useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { Cow } from "@phosphor-icons/react";
import { ArrowRight, CheckCircle2, Plus, Tag, Upload, X, type LucideIcon } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { usePageTour } from "@/hooks/usePageTour";
import { AppTour } from "../shell/AppTour";
import type { Step } from "react-joyride";
import { describeApiError } from "../../lib/apiErrors";
import { formatSex } from "../../lib/formatters";
import { ImportSheet } from "../../features/onboarding/ImportSheet";
import { InfoTooltip } from "../../components/InfoTooltip";
import { Badge } from "../../components/ui/badge";
import { Input } from "../../components/ui/input";
import { Button } from "../../components/ui/button";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { DatePickerInput } from "../../components/ui/date-picker";
import { Label } from "../../components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter
} from "../../components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "../../components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "../../components/ui/table";
import { Empty, EmptyMedia, EmptyTitle, EmptyDescription } from "../../components/ui/empty";

type AnimalListResponse = {
  items: AnimalListItem[];
  total: number;
  limit: number;
  offset: number;
};

type AnimalListItem = {
  id: string;
  identifier: string;
  name: string | null;
  sex: string;
  breed: string | null;
  status: string;
  lotId: string | null;
  lotName: string | null;
};

type LotOption = { id: string; name: string };
type LotListResponse = { items: LotOption[] };

type StatusFilter = "all" | "active" | "sold" | "dead" | "transferred";

type CreateForm = {
  identifier: string;
  name: string;
  sex: string;
  breed: string;
  status: string;
  birthDate: string;
  entryDate: string;
  birthWeightKg: string;
  lotId: string;
};

const ANIMALS_TOUR_STEPS: Step[] = [
  {
    target: "[data-tour='animals-actions']",
    title: "Cadastrar animais",
    content: "Cadastre um novo animal preenchendo identificação, raça, lote e outras informações. Ou importe via planilha.",
    placement: "bottom",
  },
  {
    target: "[data-tour='animals-filters']",
    title: "Filtrar animais",
    content: "Encontre animais rapidamente filtrando por brinco ou nome, lote e status.",
    placement: "bottom",
  },
];

const EMPTY_FORM: CreateForm = {
  identifier: "",
  name: "",
  sex: "male",
  breed: "",
  status: "active",
  birthDate: "",
  entryDate: "",
  birthWeightKg: "",
  lotId: ""
};

type AnimalBadgeConfig = { classes: string; Icon: LucideIcon; label: string };

const ANIMAL_STATUS_CONFIG: Record<string, AnimalBadgeConfig> = {
  active:      { classes: "bg-emerald-100 text-emerald-800 border-emerald-200", Icon: CheckCircle2, label: "Ativo" },
  sold:        { classes: "bg-blue-100 text-blue-800 border-blue-200",          Icon: Tag,          label: "Vendido" },
  dead:        { classes: "bg-gray-100 text-gray-700 border-gray-200",          Icon: X,            label: "Morto" },
  transferred: { classes: "bg-amber-100 text-amber-800 border-amber-200",       Icon: ArrowRight,   label: "Transferido" },
};

export function AnimalsPage() {
  const { fetchProtectedJson, session } = useAuth();
  const navigate = useNavigate();
  const { run: runTour, finish: finishTour } = usePageTour("animals");

  const [animals, setAnimals] = useState<AnimalListResponse | null>(null);
  const [lots, setLots] = useState<LotOption[]>([]);
  const [search, setSearch] = useState("");
  const [lotId, setLotId] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [offset, setOffset] = useState(0);
  const [listError, setListError] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateForm>(EMPTY_FORM);
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [importOpen, setImportOpen] = useState(false);

  const deferredSearch = useDeferredValue(search);

  useEffect(() => {
    let cancelled = false;

    async function loadAnimals() {
      try {
        const data = await fetchProtectedJson<AnimalListResponse>(
          buildListPath({ search: deferredSearch, lotId, status: statusFilter, offset })
        );
        if (!cancelled) {
          setAnimals(data);
          setListError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setAnimals(null);
          setListError(describeApiError(error));
        }
      }
    }

    void loadAnimals();
    return () => {
      cancelled = true;
    };
  }, [deferredSearch, fetchProtectedJson, lotId, offset, refreshKey, statusFilter]);

  useEffect(() => {
    let cancelled = false;

    async function loadLots() {
      try {
        const data = await fetchProtectedJson<LotListResponse>("/api/v1/lots");
        if (!cancelled) setLots(data.items);
      } catch {
        // non-critical — dropdown stays empty
      }
    }

    void loadLots();
    return () => {
      cancelled = true;
    };
  }, [fetchProtectedJson]);

  const hasActiveFilters = lotId !== "" || statusFilter !== "all";
  const canWrite = session?.permissions.includes("animals.write") ?? false;

  const herdComposition = useMemo(() => {
    const items = animals?.items ?? [];
    if (items.length === 0) return null;
    const sexCounts: Record<string, number> = {};
    const statusCounts: Record<string, number> = {};
    for (const a of items) {
      sexCounts[a.sex] = (sexCounts[a.sex] ?? 0) + 1;
      statusCounts[a.status] = (statusCounts[a.status] ?? 0) + 1;
    }
    const SEX_LABELS: Record<string, string> = { male: "Macho", female: "Fêmea", unknown: "Indefinido" };
    const pieData = Object.entries(sexCounts).map(([key, value]) => ({
      name: SEX_LABELS[key] ?? key, value,
    }));
    const activeCount = statusCounts["active"] ?? 0;
    const activePct = items.length > 0 ? Math.round((activeCount / items.length) * 100) : 0;
    return { pieData, activeCount, activePct, total: items.length };
  }, [animals?.items]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();

    if (createForm.identifier.trim().length === 0) {
      setCreateError("O brinco (identificador) é obrigatório.");
      return;
    }

    setCreateSubmitting(true);
    setCreateError(null);

    const body: Record<string, string> = {
      identifier: createForm.identifier.trim(),
      sex: createForm.sex,
      status: createForm.status
    };
    if (createForm.name.trim().length > 0) body.name = createForm.name.trim();
    if (createForm.breed.trim().length > 0) body.breed = createForm.breed.trim();
    if (createForm.birthDate.trim().length > 0) body.birthDate = createForm.birthDate.trim();
    if (createForm.entryDate.trim().length > 0) body.entryDate = createForm.entryDate.trim();
    if (createForm.birthWeightKg.trim().length > 0) body.birthWeightKg = createForm.birthWeightKg.trim();
    if (createForm.lotId.length > 0) body.lotId = createForm.lotId;

    try {
      await fetchProtectedJson<{ id: string }>("/api/v1/animals", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      setCreateOpen(false);
      setCreateForm(EMPTY_FORM);
      setRefreshKey((k) => k + 1);
    } catch (error) {
      setCreateError(describeApiError(error));
    } finally {
      setCreateSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Animais</h1>
        {canWrite ? (
          <div className="flex gap-2" data-tour="animals-actions">
            <BillingGatedButton onClick={() => setImportOpen(true)} size="sm" type="button" variant="outline">
              <Upload className="h-4 w-4" />
              Importar planilha
            </BillingGatedButton>
            <BillingGatedButton onClick={() => setCreateOpen(true)} size="sm" type="button">
              <Plus className="h-4 w-4" />
              Cadastrar animal
            </BillingGatedButton>
          </div>
        ) : null}
      </div>

      <Dialog
        onOpenChange={(open) => {
          setCreateOpen(open);
          if (!open) {
            setCreateForm(EMPTY_FORM);
            setCreateError(null);
          }
        }}
        open={createOpen}
      >
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Cadastrar animal</DialogTitle>
          </DialogHeader>
          <form className="space-y-4" onSubmit={(e) => void handleCreate(e)}>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label className="flex items-center gap-1.5" htmlFor="create-identifier">
                  Brinco *
                  <InfoTooltip text="Número único do animal na fazenda — normalmente o código do brinco de orelha. Ex: 001, A-42" />
                </Label>
                <Input
                  id="create-identifier"
                  onChange={(e) => setCreateForm((f) => ({ ...f, identifier: e.target.value }))}
                  placeholder="Ex: 347"
                  value={createForm.identifier}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="create-name">Nome</Label>
                <Input
                  id="create-name"
                  onChange={(e) => setCreateForm((f) => ({ ...f, name: e.target.value }))}
                  placeholder="Ex: Trovão"
                  value={createForm.name}
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Sexo *</Label>
                <Select
                  onValueChange={(v) => setCreateForm((f) => ({ ...f, sex: v }))}
                  value={createForm.sex}
                >
                  <SelectTrigger aria-label="Select sex">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="male">Macho</SelectItem>
                    <SelectItem value="female">Fêmea</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Status</Label>
                <Select
                  onValueChange={(v) => setCreateForm((f) => ({ ...f, status: v }))}
                  value={createForm.status}
                >
                  <SelectTrigger aria-label="Select status">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="active">Ativo</SelectItem>
                    <SelectItem value="sold">Vendido</SelectItem>
                    <SelectItem value="dead">Morto</SelectItem>
                    <SelectItem value="transferred">Transferido</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="create-breed">Raça</Label>
                <Input
                  id="create-breed"
                  onChange={(e) => setCreateForm((f) => ({ ...f, breed: e.target.value }))}
                  placeholder="Ex: Nelore"
                  value={createForm.breed}
                />
              </div>
              <div className="space-y-1.5">
                <Label>Lote</Label>
                <Select
                  onValueChange={(v) => setCreateForm((f) => ({ ...f, lotId: v === "_none" ? "" : v }))}
                  value={createForm.lotId === "" ? "_none" : createForm.lotId}
                >
                  <SelectTrigger aria-label="Select lot">
                    <SelectValue placeholder="Sem lote" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="_none">Sem lote</SelectItem>
                    {lots.map((l) => (
                      <SelectItem key={l.id} value={l.id}>{l.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="create-birth-date">Data de nascimento</Label>
                <DatePickerInput
                  id="create-birth-date"
                  onChange={(v) => setCreateForm((f) => ({ ...f, birthDate: v }))}
                  placeholder="Selecionar data"
                  value={createForm.birthDate}
                />
              </div>
              <div className="space-y-1.5">
                <Label className="flex items-center gap-1.5" htmlFor="create-entry-date">
                  Data de entrada
                  <InfoTooltip text="Data em que este animal chegou na fazenda (diferente da data de nascimento)" />
                </Label>
                <DatePickerInput
                  id="create-entry-date"
                  onChange={(v) => setCreateForm((f) => ({ ...f, entryDate: v }))}
                  placeholder="Selecionar data"
                  value={createForm.entryDate}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <Label className="flex items-center gap-1.5" htmlFor="create-birth-weight">
                Peso ao nascer (kg)
                <InfoTooltip text="Peso do animal ao nascer, em quilogramas. Deixe vazio se não souber." />
              </Label>
              <Input
                id="create-birth-weight"
                inputMode="decimal"
                onChange={(e) => setCreateForm((f) => ({ ...f, birthWeightKg: e.target.value }))}
                placeholder="Ex: 32.5"
                value={createForm.birthWeightKg}
              />
            </div>

            {createError != null ? (
              <p className="text-sm text-destructive">{createError}</p>
            ) : null}

            <DialogFooter>
              <Button
                onClick={() => setCreateOpen(false)}
                type="button"
                variant="outline"
              >
                Cancelar
              </Button>
              <Button disabled={createSubmitting} type="submit">
                {createSubmitting ? "Salvando..." : "Cadastrar"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog onOpenChange={setImportOpen} open={importOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Importar animais via planilha</DialogTitle>
          </DialogHeader>
          <ImportSheet
            dataType="animals"
            onImported={() => {
              setImportOpen(false);
              setRefreshKey((k) => k + 1);
            }}
          />
        </DialogContent>
      </Dialog>

      <div className="flex flex-wrap items-center gap-3" data-tour="animals-filters">
        <Input
          aria-label="Search animals"
          className="max-w-xs"
          onChange={(e) => {
            setSearch(e.target.value);
            setOffset(0);
          }}
          placeholder="Buscar por brinco ou nome..."
          value={search}
        />

        <select
          aria-label="Filter lot"
          className="h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-[color,box-shadow] focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none"
          onChange={(e) => {
            setLotId(e.target.value);
            setOffset(0);
          }}
          value={lotId}
        >
          <option value="">Todos os lotes</option>
          {lots.map((l) => (
            <option key={l.id} value={l.id}>
              {l.name}
            </option>
          ))}
        </select>

        <select
          aria-label="Filter status"
          className="h-9 rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-xs transition-[color,box-shadow] focus-visible:border-ring focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:outline-none"
          onChange={(e) => {
            setStatusFilter(e.target.value as StatusFilter);
            setOffset(0);
          }}
          value={statusFilter}
        >
          <option value="all">Todos os status</option>
          <option value="active">Ativo</option>
          <option value="sold">Vendido</option>
          <option value="dead">Morto</option>
          <option value="transferred">Transferido</option>
        </select>

        {hasActiveFilters ? (
          <button
            className="flex items-center gap-1 text-xs text-muted-foreground hover:text-foreground"
            onClick={() => {
              setLotId("");
              setStatusFilter("all");
              setOffset(0);
            }}
            type="button"
          >
            <X className="h-3 w-3" /> Limpar filtros
          </button>
        ) : null}
      </div>

      <p className="text-sm text-muted-foreground">
        {animals != null ? `${animals.total} animais encontrados` : "Carregando..."}
      </p>

      {herdComposition != null && (
        <div className="grid gap-4 grid-cols-1 sm:grid-cols-[160px_1fr]">
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="mb-1 text-xs font-medium text-muted-foreground">Composição do rebanho</p>
            <ResponsiveContainer width="100%" height={140}>
              <PieChart>
                <Pie data={herdComposition.pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={36} outerRadius={54}>
                  {herdComposition.pieData.map((_entry, i) => (
                    <Cell key={i} fill={i === 0 ? "var(--primary)" : "#10b981"} />
                  ))}
                </Pie>
                <Tooltip formatter={(v: number) => [v, "animais"]} />
                <Legend iconSize={10} />
              </PieChart>
            </ResponsiveContainer>
          </div>
          <div className="flex flex-col justify-center gap-3 rounded-xl border border-border bg-card p-4">
            <p className="text-xs font-medium text-muted-foreground">Status do rebanho</p>
            <div className="flex gap-6 flex-wrap">
              <div>
                <p className="text-2xl font-bold font-heading">{herdComposition.activePct}%</p>
                <p className="text-xs text-muted-foreground">{herdComposition.activeCount} ativos</p>
              </div>
              <div>
                <p className="text-2xl font-bold font-heading">{herdComposition.total - herdComposition.activeCount}</p>
                <p className="text-xs text-muted-foreground">vendidos / mortos / transferidos</p>
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="overflow-hidden rounded-xl border border-border bg-card">
        {animals == null && listError == null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">Carregando animais...</div>
        ) : animals == null && listError != null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">{listError}</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">#</TableHead>
                <TableHead>Nome</TableHead>
                <TableHead className="hidden md:table-cell">Raça</TableHead>
                <TableHead className="hidden md:table-cell">Sexo</TableHead>
                <TableHead>Lote</TableHead>
                <TableHead>Status</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {animals?.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Empty>
                      <EmptyMedia>
                        <Cow size={80} weight="duotone" className="text-muted-foreground" />
                      </EmptyMedia>
                      <EmptyTitle>Nenhum animal encontrado</EmptyTitle>
                      <EmptyDescription>Nenhum animal corresponde aos filtros aplicados.</EmptyDescription>
                    </Empty>
                  </TableCell>
                </TableRow>
              ) : (
                animals?.items.map((animal) => (
                  <TableRow
                    key={animal.id}
                    aria-label={`View ${animal.name ?? animal.identifier}`}
                    className="cursor-pointer"
                    onClick={() => navigate(`/app/animals/${animal.id}`)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") navigate(`/app/animals/${animal.id}`);
                    }}
                    role="button"
                    tabIndex={0}
                  >
                    <TableCell className="pl-4 font-mono font-semibold text-emerald-700">
                      #{animal.identifier}
                    </TableCell>
                    <TableCell className="font-medium">
                      {animal.name ?? `Animal ${animal.identifier}`}
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {animal.breed ?? "—"}
                    </TableCell>
                    <TableCell className="hidden text-muted-foreground md:table-cell">
                      {formatSex(animal.sex)}
                    </TableCell>
                    <TableCell className="text-muted-foreground">{animal.lotName ?? "—"}</TableCell>
                    <TableCell>
                      <AnimalStatusBadge status={animal.status} />
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {animals != null ? (
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">
            {animals.total === 0
              ? "Sem resultados"
              : `Exibindo ${animals.offset + 1}–${Math.min(animals.offset + animals.limit, animals.total)} de ${animals.total}`}
          </span>
          <div className="flex gap-2">
            <Button
              disabled={animals.offset === 0}
              onClick={() => setOffset(Math.max(0, offset - animals.limit))}
              size="sm"
              type="button"
              variant="outline"
            >
              Anterior
            </Button>
            <Button
              disabled={animals.offset + animals.limit >= animals.total}
              onClick={() => setOffset(offset + animals.limit)}
              size="sm"
              type="button"
              variant="outline"
            >
              Próximo
            </Button>
          </div>
        </div>
      ) : null}
      <AppTour steps={ANIMALS_TOUR_STEPS} run={runTour} onFinish={finishTour} />
    </div>
  );
}

function AnimalStatusBadge({ status }: { status: string }) {
  const cfg = ANIMAL_STATUS_CONFIG[status];
  if (cfg == null) return null;
  return (
    <Badge className={cfg.classes} variant="outline">
      <cfg.Icon aria-hidden="true" />
      {cfg.label}
    </Badge>
  );
}

function buildListPath(filters: { search: string; lotId: string; status: StatusFilter; offset: number }) {
  const params = new URLSearchParams({ limit: "20", offset: String(filters.offset) });

  if (filters.search.trim().length > 0) params.set("search", filters.search.trim());
  if (filters.lotId.length > 0) params.set("lot", filters.lotId);
  if (filters.status !== "all") params.set("status", filters.status);

  return `/api/v1/animals?${params.toString()}`;
}

