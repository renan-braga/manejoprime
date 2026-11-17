import { useEffect, useMemo, useState } from "react";
import {
  PieChart, Pie, Cell, AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, Legend,
} from "recharts";
import { Grains } from "@phosphor-icons/react";
import { Upload } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import { ImportSheet } from "../../features/onboarding/ImportSheet";
import { InfoTooltip } from "../../components/InfoTooltip";
import { Badge } from "../../components/ui/badge";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { DatePickerInput } from "../../components/ui/date-picker";
import {
  Dialog,
  DialogContent,
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

type FeedingListResponse = {
  items: FeedingRecord[];
  total: number;
  limit: number;
  offset: number;
};

type FeedingRecord = {
  id: string;
  animalId: string | null;
  lotId: string | null;
  feedType: string;
  description: string | null;
  quantityKg: number;
  costPerKg: number | null;
  startDate: string;
  endDate: string | null;
  source: string;
  recordedBy: string;
};

type FeedingTarget = "animal" | "lot";

type FeedingFormState = {
  feedType: string;
  description: string;
  quantityKg: string;
  costPerKg: string;
  startDate: string;
  endDate: string;
};

const DEFAULT_FEEDING_FORM: FeedingFormState = {
  feedType: "racao",
  description: "",
  quantityKg: "",
  costPerKg: "",
  startDate: "",
  endDate: "",
};

const selectClass =
  "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring";

export function FeedingPage() {
  const { fetchProtectedJson, session } = useAuth();
  const permissions = session?.permissions ?? [];
  const canManageFeedings = permissions.includes("feeding.write");
  const [feedingTarget, setFeedingTarget] = useState<FeedingTarget>("animal");
  const [feedingSearch, setFeedingSearch] = useState("");
  const [feedTypeFilter, setFeedTypeFilter] = useState("all");
  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [lots, setLots] = useState<LotOption[]>([]);
  const [selectedAnimalId, setSelectedAnimalId] = useState("");
  const [selectedLotId, setSelectedLotId] = useState("");
  const [feedings, setFeedings] = useState<FeedingListResponse | null>(null);
  const [loadingOptions, setLoadingOptions] = useState(false);
  const [loadingFeedings, setLoadingFeedings] = useState(false);
  const [optionsError, setOptionsError] = useState<string | null>(null);
  const [feedingsError, setFeedingsError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [feedingForm, setFeedingForm] = useState<FeedingFormState>(DEFAULT_FEEDING_FORM);
  const [saveStatus, setSaveStatus] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  useEffect(() => {
    if (!canManageFeedings) return;
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
        if (!cancelled) setOptionsError(describeApiError(error, "We could not complete the feeding request."));
      } finally {
        if (!cancelled) setLoadingOptions(false);
      }
    }

    void loadOptions();
    return () => { cancelled = true; };
  }, [canManageFeedings, fetchProtectedJson]);

  useEffect(() => {
    if (!canManageFeedings) return;
    const targetId = feedingTarget === "animal" ? selectedAnimalId : selectedLotId;
    if (targetId.length === 0) { setFeedings(null); return; }
    let cancelled = false;

    async function loadFeedings() {
      setLoadingFeedings(true);
      try {
        const path =
          feedingTarget === "animal"
            ? `/api/v1/feedings?animalId=${selectedAnimalId}&limit=20&offset=0`
            : `/api/v1/feedings?lotId=${selectedLotId}&limit=20&offset=0`;
        const nextFeedings = await fetchProtectedJson<FeedingListResponse>(path);
        if (!cancelled) { setFeedings(nextFeedings); setFeedingsError(null); }
      } catch (error) {
        if (!cancelled) { setFeedings(null); setFeedingsError(describeApiError(error, "We could not complete the feeding request.")); }
      } finally {
        if (!cancelled) setLoadingFeedings(false);
      }
    }

    void loadFeedings();
    return () => { cancelled = true; };
  }, [canManageFeedings, feedingTarget, fetchProtectedJson, selectedAnimalId, selectedLotId]);

  const selectedAnimal = animals.find((a) => a.id === selectedAnimalId) ?? null;
  const selectedLot = lots.find((l) => l.id === selectedLotId) ?? null;

  const activeContext = useMemo(() => {
    if (feedingTarget === "animal") {
      return selectedAnimal == null ? "Selecione um animal" : describeAnimalContext(selectedAnimal);
    }
    return selectedLot == null ? "Selecione um lote" : describeLotContext(selectedLot);
  }, [feedingTarget, selectedAnimal, selectedLot]);

  const filteredFeedings = useMemo(() => {
    const items = feedings?.items ?? [];
    const normalizedSearch = feedingSearch.trim().toLowerCase();

    return items.filter((feeding) => {
      const matchesType = feedTypeFilter === "all" || feeding.feedType === feedTypeFilter;
      const searchable = [
        feeding.description,
        formatFeedType(feeding.feedType),
        formatSource(feeding.source),
        feeding.recordedBy,
        activeContext,
      ]
        .filter((value): value is string => value != null)
        .join(" ")
        .toLowerCase();

      return matchesType && (normalizedSearch.length === 0 || searchable.includes(normalizedSearch));
    });
  }, [activeContext, feedTypeFilter, feedingSearch, feedings?.items]);

  const visibleFeedTypes = useMemo(() => {
    const types = new Set((feedings?.items ?? []).map((feeding) => feeding.feedType));
    return Array.from(types).sort((left, right) => formatFeedType(left).localeCompare(formatFeedType(right)));
  }, [feedings?.items]);

  const totalDailyCost = filteredFeedings.reduce(
    (sum, feeding) => sum + (feeding.costPerKg == null ? 0 : feeding.quantityKg * feeding.costPerKg),
    0
  );

  async function submitFeeding() {
    if (!canManageFeedings) return;
    setSubmitting(true);
    setSaveStatus(null);
    setFieldErrors({});
    const payload = {
      animalId: feedingTarget === "animal" ? selectedAnimalId : undefined,
      lotId: feedingTarget === "lot" ? selectedLotId : undefined,
      feedType: feedingForm.feedType,
      description: feedingForm.description || undefined,
      quantityKg: feedingForm.quantityKg,
      costPerKg: feedingForm.costPerKg || undefined,
      startDate: feedingForm.startDate,
      endDate: feedingForm.endDate || undefined,
    };
    try {
      const created = await fetchProtectedJson<FeedingRecord>("/api/v1/feedings", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      setFeedings((current) => ({
        items: [created, ...(current?.items ?? [])],
        total: (current?.total ?? 0) + 1,
        limit: current?.limit ?? 20,
        offset: current?.offset ?? 0,
      }));
      setFeedingForm(DEFAULT_FEEDING_FORM);
      setSaveStatus("Alimentação salva.");
    } catch (error) {
      const fe = readFieldErrors(error);
      setFieldErrors(fe);
      setSaveStatus(Object.keys(fe).length > 0 ? "Check the highlighted fields and try again." : describeApiError(error, "We could not complete the feeding request."));
    } finally {
      setSubmitting(false);
    }
  }

  if (!canManageFeedings) {
    return (
      <div className="space-y-4">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Planejamento de alimentação disponível apenas para administradores.</h1>
        <div className="rounded-lg border border-border bg-card p-6">
          <p className="text-sm text-muted-foreground">
            Esta rota está dentro do módulo de animais para contexto compartilhado, mas apenas sessões com acesso de administrador podem registrar e inspecionar planos de alimentação.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Alimentações</h1>
        {canManageFeedings ? (
          <BillingGatedButton onClick={() => setImportOpen(true)} size="sm" type="button" variant="outline">
            <Upload className="h-4 w-4" />
            Importar planilha
          </BillingGatedButton>
        ) : null}
      </div>

      <Dialog onOpenChange={setImportOpen} open={importOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Importar alimentações via planilha</DialogTitle>
          </DialogHeader>
          <ImportSheet
            dataType="feedings"
            onImported={() => {
              setImportOpen(false);
            }}
          />
        </DialogContent>
      </Dialog>

      {optionsError != null && (
        <p className="text-sm text-destructive">{optionsError}</p>
      )}

      <div className="grid gap-3 sm:grid-cols-3">
        <SummaryTile label="Registros visíveis" value={filteredFeedings.length.toString()} />
        <SummaryTile
          label="Quantidade diária"
          value={`${filteredFeedings.reduce((sum, feeding) => sum + feeding.quantityKg, 0).toFixed(1)} kg`}
        />
        <SummaryTile label="Custo diário" value={totalDailyCost > 0 ? `R$ ${totalDailyCost.toFixed(2)}` : "Não informado"} />
      </div>

      {/* Register form */}
      <div className="overflow-hidden rounded-lg border border-border bg-card p-6 space-y-4">
        <div className="flex items-center justify-between flex-wrap gap-4">
          <div>
            <h2 className="font-heading text-lg font-semibold">Registrar alimentação</h2>
            <p className="text-sm text-muted-foreground">Escolha o alvo e registre a janela de alimentação.</p>
          </div>
          <div className="flex rounded-md border border-border overflow-hidden" role="group" aria-label="Feeding target">
            <button
              className={`px-4 py-1.5 text-sm transition-colors ${feedingTarget === "animal" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
              onClick={() => setFeedingTarget("animal")}
              type="button"
            >
              Animal
            </button>
            <button
              className={`px-4 py-1.5 text-sm transition-colors ${feedingTarget === "lot" ? "bg-primary text-primary-foreground" : "text-muted-foreground hover:bg-muted"}`}
              onClick={() => setFeedingTarget("lot")}
              type="button"
            >
              Lote
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="space-y-1.5">
            <Label htmlFor="target-select">{feedingTarget === "animal" ? "Animal" : "Lote"}</Label>
            <select
              aria-label={feedingTarget === "animal" ? "Animal" : "Lote"}
              className={selectClass}
              disabled={loadingOptions}
              id="target-select"
              onChange={(e) =>
                feedingTarget === "animal" ? setSelectedAnimalId(e.target.value) : setSelectedLotId(e.target.value)
              }
              value={feedingTarget === "animal" ? selectedAnimalId : selectedLotId}
            >
              {(feedingTarget === "animal" ? animals : lots).map((option) => (
                <option key={option.id} value={option.id}>
                  {"identifier" in option ? formatAnimalOption(option) : formatLotOption(option)}
                </option>
              ))}
            </select>
            {fieldErrors.target && <p className="text-xs text-destructive">{fieldErrors.target}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="feed-type">
              Tipo de ração
              <InfoTooltip text="Ração: concentrado industrializado · Silagem: forragem fermentada (milho, cana) · Suplemento: vitaminas e minerais · Sal mineral: sal mineralizado · Pasto: pastagem · Misto: mistura de tipos" />
            </Label>
            <select
              aria-label="Feed type"
              className={selectClass}
              id="feed-type"
              onChange={(e) => setFeedingForm((f) => ({ ...f, feedType: e.target.value }))}
              value={feedingForm.feedType}
            >
              <option value="pasto">Pasto</option>
              <option value="racao">Ração</option>
              <option value="silagem">Silagem</option>
              <option value="suplemento">Suplemento</option>
              <option value="sal_mineral">Sal mineral</option>
              <option value="misto">Misto</option>
            </select>
            {fieldErrors.feedType && <p className="text-xs text-destructive">{fieldErrors.feedType}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="description">Descrição</Label>
            <Input
              aria-label="Description"
              id="description"
              onChange={(e) => setFeedingForm((f) => ({ ...f, description: e.target.value }))}
              value={feedingForm.description}
            />
            {fieldErrors.description && <p className="text-xs text-destructive">{fieldErrors.description}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="quantity-kg">
              Quantidade (kg)
              <InfoTooltip text="Quantidade total de alimento fornecida por dia para este animal ou lote inteiro" />
            </Label>
            <Input
              aria-label="Quantity (kg)"
              id="quantity-kg"
              onChange={(e) => setFeedingForm((f) => ({ ...f, quantityKg: e.target.value }))}
              type="number"
              value={feedingForm.quantityKg}
            />
            {fieldErrors.quantityKg && <p className="text-xs text-destructive">{fieldErrors.quantityKg}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="cost-per-kg">
              Custo por kg
              <InfoTooltip text="Quanto custa 1 kg deste alimento. Usado para calcular o custo total do lote." />
            </Label>
            <Input
              aria-label="Cost per kg"
              id="cost-per-kg"
              onChange={(e) => setFeedingForm((f) => ({ ...f, costPerKg: e.target.value }))}
              type="number"
              value={feedingForm.costPerKg}
            />
            {fieldErrors.costPerKg && <p className="text-xs text-destructive">{fieldErrors.costPerKg}</p>}
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="start-date">Data de início</Label>
            <DatePickerInput
              id="start-date"
              onChange={(v) => setFeedingForm((f) => ({ ...f, startDate: v }))}
              placeholder="Selecionar data"
              value={feedingForm.startDate}
            />
            {fieldErrors.startDate && <p className="text-xs text-destructive">{fieldErrors.startDate}</p>}
          </div>

          <div className="space-y-1.5">
            <Label className="flex items-center gap-1.5" htmlFor="end-date">
              Data de fim
              <InfoTooltip text="Data em que parou de fornecer este alimento. Deixe vazio se ainda está sendo dado." />
            </Label>
            <DatePickerInput
              id="end-date"
              onChange={(v) => setFeedingForm((f) => ({ ...f, endDate: v }))}
              placeholder="Selecionar data (opcional)"
              value={feedingForm.endDate}
            />
            {fieldErrors.endDate && <p className="text-xs text-destructive">{fieldErrors.endDate}</p>}
          </div>
        </div>

        {saveStatus != null && (
          <p className="text-sm text-muted-foreground">{saveStatus}</p>
        )}
        <BillingGatedButton
          disabled={submitting}
          onClick={() => { void submitFeeding(); }}
          type="button"
        >
          {submitting ? "Salvando..." : "Salvar alimentação"}
        </BillingGatedButton>
      </div>

      {filteredFeedings.length > 0 && (() => {
        const typeTotals = filteredFeedings.reduce<Record<string, number>>((acc, f) => {
          acc[f.feedType] = (acc[f.feedType] ?? 0) + f.quantityKg;
          return acc;
        }, {});
        const pieData = Object.entries(typeTotals).map(([key, value]) => ({
          name: formatFeedType(key), value,
        }));
        const PIE_COLORS = ["var(--primary)", "#10b981", "#f59e0b", "#6366f1", "#ec4899", "#14b8a6"];
        const hasCost = filteredFeedings.some((f) => f.costPerKg != null);
        const areaData = hasCost
          ? [...filteredFeedings]
              .sort((a, b) => a.startDate.localeCompare(b.startDate))
              .map((f) => ({
                date: f.startDate.slice(5).replace("-", "/"),
                custo: f.costPerKg != null ? +(f.quantityKg * f.costPerKg).toFixed(2) : 0,
              }))
          : [];
        return (
          <div className={`grid gap-4 ${hasCost ? "grid-cols-1 sm:grid-cols-2" : "grid-cols-1 max-w-xs"}`}>
            <div className="rounded-xl border border-border bg-card p-4">
              <p className="mb-2 text-xs font-medium text-muted-foreground">Distribuição por tipo</p>
              <ResponsiveContainer width="100%" height={160}>
                <PieChart>
                  <Pie data={pieData} dataKey="value" nameKey="name" cx="50%" cy="50%" innerRadius={40} outerRadius={64}>
                    {pieData.map((_entry, i) => (
                      <Cell key={i} fill={PIE_COLORS[i % PIE_COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => [`${v.toFixed(1)} kg`]} />
                  <Legend iconSize={10} />
                </PieChart>
              </ResponsiveContainer>
            </div>
            {hasCost && (
              <div className="rounded-xl border border-border bg-card p-4">
                <p className="mb-2 text-xs font-medium text-muted-foreground">Custo por período (R$)</p>
                <ResponsiveContainer width="100%" height={160}>
                  <AreaChart data={areaData} margin={{ top: 4, right: 12, left: 0, bottom: 0 }}>
                    <XAxis dataKey="date" tick={{ fontSize: 11 }} stroke="var(--border)" />
                    <YAxis tick={{ fontSize: 11 }} stroke="var(--border)" width={44} />
                    <Tooltip formatter={(v: number) => [`R$ ${v.toFixed(2)}`, "Custo"]} />
                    <Area type="monotone" dataKey="custo" stroke="var(--primary)" fill="var(--primary)" fillOpacity={0.15} strokeWidth={2} />
                  </AreaChart>
                </ResponsiveContainer>
              </div>
            )}
          </div>
        );
      })()}

      {/* Feeding history table */}
      <div>
        <div className="mb-3 flex flex-wrap items-end justify-between gap-3">
          <div>
            <h2 className="font-heading text-lg font-semibold">Histórico de alimentações</h2>
            <p className="text-sm text-muted-foreground">{activeContext}</p>
          </div>
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-1.5">
              <Label htmlFor="feeding-search">Buscar</Label>
              <Input
                aria-label="Buscar alimentações"
                id="feeding-search"
                onChange={(e) => setFeedingSearch(e.target.value)}
                placeholder="Descrição, tipo, fonte"
                value={feedingSearch}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="feed-type-filter">Tipo de ração</Label>
              <select
                aria-label="Feed type filter"
                className={selectClass}
                id="feed-type-filter"
                onChange={(e) => setFeedTypeFilter(e.target.value)}
                value={feedTypeFilter}
              >
                <option value="all">Todos os tipos</option>
                {visibleFeedTypes.map((feedType) => (
                  <option key={feedType} value={feedType}>
                    {formatFeedType(feedType)}
                  </option>
                ))}
              </select>
            </div>
          </div>
        </div>
        <div className="overflow-hidden rounded-lg border border-border bg-card">
          {loadingFeedings || loadingOptions ? (
            <div className="p-4 space-y-2" data-testid="feedings-skeleton">
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-full" />
              <Skeleton className="h-8 w-3/4" />
            </div>
          ) : feedingsError != null ? (
            <div className="p-8 text-center text-sm text-muted-foreground">{feedingsError}</div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="pl-4">Descrição</TableHead>
                  <TableHead>Tipo</TableHead>
                  <TableHead>Quantidade</TableHead>
                  <TableHead>Custo</TableHead>
                  <TableHead>Período</TableHead>
                  <TableHead>Fonte</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {feedings == null || filteredFeedings.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6}>
                      <Empty>
                        <EmptyMedia>
                          <Grains size={80} weight="duotone" className="text-muted-foreground" />
                        </EmptyMedia>
                        <EmptyTitle>Nenhuma alimentação registrada</EmptyTitle>
                        <EmptyDescription>Registre a primeira janela de alimentação para este animal ou lote.</EmptyDescription>
                      </Empty>
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredFeedings.map((feeding) => (
                    <TableRow key={feeding.id}>
                      <TableCell className="pl-4 font-medium">
                        {feeding.description ?? formatFeedType(feeding.feedType)}
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">{formatFeedType(feeding.feedType)}</Badge>
                      </TableCell>
                      <TableCell>{formatQuantity(feeding.quantityKg)}</TableCell>
                      <TableCell>{formatCost(feeding.costPerKg)}</TableCell>
                      <TableCell>{describeFeedingWindow(feeding.startDate, feeding.endDate)}</TableCell>
                      <TableCell>{formatSource(feeding.source)}</TableCell>
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

function SummaryTile({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border bg-card p-4">
      <p className="text-xs font-medium text-muted-foreground">{label}</p>
      <p className="mt-1 font-heading text-xl font-bold">{value}</p>
    </div>
  );
}

function describeAnimalContext(animal: AnimalOption) {
  return `Animal ${animal.identifier} · ${animal.name ?? "Sem nome"}`;
}

function describeLotContext(lot: LotOption) {
  return `Lote ${lot.name} · ${formatPurpose(lot.purpose)}`;
}

function describeFeedingWindow(startDate: string, endDate: string | null) {
  if (endDate == null || endDate.length === 0) return "Janela aberta";
  return `${startDate} até ${endDate}`;
}

function formatAnimalOption(animal: AnimalOption) {
  return `${animal.identifier} · ${animal.name ?? "Sem nome"} · ${formatStatus(animal.status)}`;
}

function formatLotOption(lot: LotOption) {
  return `${lot.name} · ${formatPurpose(lot.purpose)} · ${lot.animalsCount} animais`;
}

function formatFeedType(feedType: string) {
  switch (feedType) {
    case "pasto": return "Pasto";
    case "racao": return "Ração";
    case "silagem": return "Silagem";
    case "suplemento": return "Suplemento";
    case "sal_mineral": return "Sal mineral";
    case "misto": return "Misto";
    default: return feedType;
  }
}

function formatPurpose(purpose: string) {
  switch (purpose) {
    case "breeding": return "Cria";
    case "raising": return "Recria";
    case "fattening": return "Engorda";
    case "quarantine": return "Quarentena";
    default: return purpose;
  }
}

function formatStatus(status: string) {
  return status.charAt(0).toUpperCase() + status.slice(1);
}

function formatQuantity(quantityKg: number) {
  return `${quantityKg.toString().replace(/\.0$/, "")} kg`;
}

function formatCost(costPerKg: number | null) {
  return costPerKg == null ? "Custo não informado" : `R$ ${costPerKg.toFixed(2)}/kg`;
}

function formatSource(source: string) {
  return source === "app" ? "App" : source;
}

