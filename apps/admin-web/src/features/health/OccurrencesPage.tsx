import { useEffect, useMemo, useState } from "react";
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, Cell,
} from "recharts";
import { FirstAid } from "@phosphor-icons/react";
import { AlertTriangle, ArrowDown, ArrowUp, CheckCircle2, Circle, Clock, Minus, Plus, Trash2, type LucideIcon } from "lucide-react";
import { describeApiError } from "../../lib/apiErrors";
import { capitalize, formatOccurrenceStatus, formatOccurrenceSource } from "../../lib/formatters";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { Skeleton } from "../../components/ui/skeleton";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "../../components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { useAuth } from "../auth/AuthContext";
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

type OccurrenceListResponse = {
  items: OccurrenceRecord[];
  total: number;
  limit: number;
  offset: number;
};

type OccurrenceRecord = {
  id: string;
  animalId: string;
  animalIdentifier: string;
  animalName: string | null;
  type: string;
  severity: string;
  status: string;
  description: string;
  photoUrl: string | null;
  source: string;
  recordedBy: string;
  resolvedBy: string | null;
  createdAt: string;
  updatedAt: string;
};

type OccurrenceStatusFilter = "all" | "open" | "in_follow_up" | "resolved";
type OccurrenceSeverityFilter = "all" | "low" | "medium" | "high" | "critical";

type CreateForm = { animalId: string; type: string; severity: string; description: string };
const EMPTY_FORM: CreateForm = { animalId: "", type: "health", severity: "low", description: "" };

type BadgeConfig = { classes: string; Icon: LucideIcon; label: string };

const SEVERITY_CONFIG: Record<string, BadgeConfig> = {
  low:      { classes: "bg-blue-100 text-blue-800 border-blue-200",       Icon: ArrowDown,     label: "Baixa" },
  medium:   { classes: "bg-yellow-100 text-yellow-800 border-yellow-300", Icon: Minus,         label: "Média" },
  high:     { classes: "bg-orange-100 text-orange-800 border-orange-200", Icon: ArrowUp,       label: "Alta" },
  critical: { classes: "bg-red-100 text-red-800 border-red-200",          Icon: AlertTriangle, label: "Crítica" },
};

const STATUS_CONFIG: Record<string, BadgeConfig> = {
  open:         { classes: "bg-emerald-100 text-emerald-800 border-emerald-200", Icon: Circle,       label: "Aberta" },
  in_follow_up: { classes: "bg-yellow-100 text-yellow-800 border-yellow-300",   Icon: Clock,        label: "Em acompanhamento" },
  resolved:     { classes: "bg-gray-100 text-gray-700 border-gray-200",         Icon: CheckCircle2, label: "Resolvida" },
};

const selectClass =
  "flex h-9 w-full rounded-md border border-input bg-transparent px-3 py-1 text-sm shadow-sm transition-colors focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:border-ring";

export function OccurrencesPage() {
  const { fetchProtectedJson, session } = useAuth();
  const permissions = session?.permissions ?? [];
  const canManageOccurrences = permissions.includes("occurrences.write");
  const canResolveOccurrences = session?.role === "admin" || session?.role === "veterinarian";

  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [selectedAnimalId, setSelectedAnimalId] = useState("");
  const [occurrences, setOccurrences] = useState<OccurrenceListResponse | null>(null);
  const [selectedOccurrenceId, setSelectedOccurrenceId] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<OccurrenceStatusFilter>("all");
  const [severityFilter, setSeverityFilter] = useState<OccurrenceSeverityFilter>("all");
  const [loadingAnimals, setLoadingAnimals] = useState(false);
  const [loadingOccurrences, setLoadingOccurrences] = useState(false);
  const [animalsError, setAnimalsError] = useState<string | null>(null);
  const [occurrencesError, setOccurrencesError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [actionStatus, setActionStatus] = useState<string | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);

  const [createOpen, setCreateOpen] = useState(false);
  const [createForm, setCreateForm] = useState<CreateForm>(EMPTY_FORM);
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [deleteTarget, setDeleteTarget] = useState<{ id: string; label: string } | null>(null);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    if (!canManageOccurrences) return;
    let cancelled = false;

    async function loadAnimals() {
      setLoadingAnimals(true);
      try {
        const response = await fetchProtectedJson<AnimalListResponse>("/api/v1/animals?limit=100&offset=0");
        if (cancelled) return;
        setAnimals(response.items);
        setAnimalsError(null);
      } catch (error) {
        if (!cancelled) setAnimalsError(describeApiError(error, "We could not load the current occurrence data."));
      } finally {
        if (!cancelled) setLoadingAnimals(false);
      }
    }

    void loadAnimals();
    return () => { cancelled = true; };
  }, [canManageOccurrences, fetchProtectedJson]);

  useEffect(() => {
    if (!canManageOccurrences) return;
    let cancelled = false;

    async function loadOccurrences() {
      setLoadingOccurrences(true);
      try {
        const searchParams = new URLSearchParams({ limit: "20", offset: "0" });
        if (selectedAnimalId.length > 0) searchParams.set("animalId", selectedAnimalId);

        const response = await fetchProtectedJson<OccurrenceListResponse>(
          `/api/v1/occurrences?${searchParams.toString()}`
        );
        if (cancelled) return;
        setOccurrences(response);
        setSelectedOccurrenceId((current) => {
          if (response.items.some((occurrence) => occurrence.id === current)) return current;
          return response.items[0]?.id ?? "";
        });
        setOccurrencesError(null);
      } catch (error) {
        if (!cancelled) {
          setOccurrences(null);
          setSelectedOccurrenceId("");
          setOccurrencesError(describeApiError(error, "We could not load the current occurrence data."));
        }
      } finally {
        if (!cancelled) setLoadingOccurrences(false);
      }
    }

    void loadOccurrences();
    return () => { cancelled = true; };
  }, [canManageOccurrences, fetchProtectedJson, refreshKey, selectedAnimalId]);

  const filteredOccurrences = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();
    return (occurrences?.items ?? []).filter((occurrence) => {
      const matchesStatus = statusFilter === "all" || occurrence.status === statusFilter;
      const matchesSeverity = severityFilter === "all" || occurrence.severity === severityFilter;
      const searchable = [
        describeOccurrenceContext(occurrence),
        occurrence.description,
        capitalize(occurrence.type),
        formatOccurrenceSeverity(occurrence.severity),
        formatOccurrenceStatus(occurrence.status),
        formatOccurrenceSource(occurrence.source),
        occurrence.recordedBy,
      ].join(" ").toLowerCase();

      return matchesStatus && matchesSeverity && (normalizedSearch.length === 0 || searchable.includes(normalizedSearch));
    });
  }, [occurrences?.items, search, severityFilter, statusFilter]);

  const selectedOccurrence =
    filteredOccurrences.find((occurrence) => occurrence.id === selectedOccurrenceId) ?? filteredOccurrences[0] ?? null;

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (createForm.animalId.length === 0) {
      setCreateError("Selecione um animal.");
      return;
    }
    if (createForm.description.trim().length === 0) {
      setCreateError("A descrição é obrigatória.");
      return;
    }
    setCreateSubmitting(true);
    setCreateError(null);
    try {
      await fetchProtectedJson<OccurrenceRecord>("/api/v1/occurrences", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          animalId: createForm.animalId,
          type: createForm.type,
          severity: createForm.severity,
          description: createForm.description.trim()
        })
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

  async function handleDeleteOccurrence() {
    if (deleteTarget == null) return;
    setDeleting(true);
    try {
      await fetchProtectedJson(`/api/v1/occurrences/${deleteTarget.id}`, { method: "DELETE" });
      setDeleteTarget(null);
      setRefreshKey((k) => k + 1);
    } catch (error) {
      setActionStatus(describeApiError(error));
      setDeleteTarget(null);
    } finally {
      setDeleting(false);
    }
  }

  async function runOccurrenceAction(action: "resolve" | "reopen") {
    if (!canManageOccurrences || !canResolveOccurrences || selectedOccurrence == null) return;
    setSubmitting(true);
    setActionStatus(null);
    try {
      await fetchProtectedJson(`/api/v1/occurrences/${selectedOccurrence.id}/${action}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: action === "resolve" ? JSON.stringify({ note: "Resolved from admin web." }) : undefined,
      });
      setActionStatus(action === "resolve" ? "Ocorrência resolvida." : "Ocorrência reaberta.");
      setRefreshKey((current) => current + 1);
    } catch (error) {
      setActionStatus(describeApiError(error, "We could not load the current occurrence data."));
    } finally {
      setSubmitting(false);
    }
  }

  if (!canManageOccurrences) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Ocorrências</h1>
        <BillingGatedButton onClick={() => { setCreateForm({ ...EMPTY_FORM, animalId: selectedAnimalId }); setCreateOpen(true); }} size="sm" type="button">
          <Plus className="h-4 w-4" />
          Registrar ocorrência
        </BillingGatedButton>
      </div>

      <Dialog onOpenChange={(open) => { setCreateOpen(open); if (!open) { setCreateForm(EMPTY_FORM); setCreateError(null); } }} open={createOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader><DialogTitle>Registrar ocorrência</DialogTitle></DialogHeader>
          <form className="space-y-4" onSubmit={(e) => void handleCreate(e)}>
            <div className="space-y-1.5">
              <Label htmlFor="occ-animal">Animal *</Label>
              <select
                aria-label="Animal"
                className={selectClass}
                id="occ-animal"
                onChange={(e) => setCreateForm((f) => ({ ...f, animalId: e.target.value }))}
                value={createForm.animalId}
              >
                <option value="">Selecionar animal</option>
                {animals.map((a) => (
                  <option key={a.id} value={a.id}>{formatAnimalOption(a)}</option>
                ))}
              </select>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="occ-type">Tipo *</Label>
                <select
                  aria-label="Tipo"
                  className={selectClass}
                  id="occ-type"
                  onChange={(e) => setCreateForm((f) => ({ ...f, type: e.target.value }))}
                  value={createForm.type}
                >
                  <option value="health">Saúde</option>
                  <option value="behavior">Comportamento</option>
                  <option value="injury">Ferimento</option>
                  <option value="parasite">Parasita</option>
                  <option value="reproduction">Reprodução</option>
                  <option value="nutrition">Nutrição</option>
                  <option value="other">Outro</option>
                </select>
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="occ-severity">Gravidade *</Label>
                <select
                  aria-label="Gravidade"
                  className={selectClass}
                  id="occ-severity"
                  onChange={(e) => setCreateForm((f) => ({ ...f, severity: e.target.value }))}
                  value={createForm.severity}
                >
                  <option value="low">Baixa</option>
                  <option value="medium">Média</option>
                  <option value="high">Alta</option>
                  <option value="critical">Crítica</option>
                </select>
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="occ-description">Descrição *</Label>
              <textarea
                className="flex min-h-[80px] w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm shadow-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-[3px] focus-visible:ring-ring/50 focus-visible:border-ring"
                id="occ-description"
                onChange={(e) => setCreateForm((f) => ({ ...f, description: e.target.value }))}
                placeholder="Descreva o evento de saúde observado..."
                value={createForm.description}
              />
            </div>
            {createError != null ? <p className="text-sm text-destructive">{createError}</p> : null}
            <DialogFooter>
              <Button onClick={() => setCreateOpen(false)} type="button" variant="outline">Cancelar</Button>
              <Button disabled={createSubmitting} type="submit">{createSubmitting ? "Salvando..." : "Registrar"}</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog onOpenChange={(open) => { if (!open) setDeleteTarget(null); }} open={deleteTarget != null}>
        <DialogContent className="max-w-sm">
          <DialogHeader><DialogTitle>Confirmar exclusão</DialogTitle></DialogHeader>
          <p className="text-sm text-muted-foreground">
            Tem certeza que deseja excluir <span className="font-medium text-foreground">{deleteTarget?.label}</span>? Esta ação não pode ser desfeita.
          </p>
          <DialogFooter>
            <Button onClick={() => setDeleteTarget(null)} type="button" variant="outline">Cancelar</Button>
            <Button disabled={deleting} onClick={() => { void handleDeleteOccurrence(); }} type="button" variant="destructive">
              {deleting ? "Excluindo..." : "Excluir"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {animalsError != null && (
        <p className="text-sm text-destructive">{animalsError}</p>
      )}

      <div className="grid gap-3 sm:grid-cols-3">
        <SummaryTile label="Ocorrências visíveis" value={filteredOccurrences.length.toString()} />
        <SummaryTile label="Abertas" value={filteredOccurrences.filter((occurrence) => occurrence.status === "open").length.toString()} />
        <SummaryTile
          label="Alta ou crítica"
          value={filteredOccurrences.filter((occurrence) => ["high", "critical"].includes(occurrence.severity)).length.toString()}
        />
      </div>

      {filteredOccurrences.length > 0 && (() => {
        const severityOrder = ["low", "medium", "high", "critical"];
        const SEVERITY_COLORS: Record<string, string> = {
          low: "#3b82f6", medium: "#eab308", high: "#f97316", critical: "#ef4444",
        };
        const SEVERITY_LABELS: Record<string, string> = {
          low: "Baixa", medium: "Média", high: "Alta", critical: "Crítica",
        };
        const counts = severityOrder.map((s) => ({
          gravidade: SEVERITY_LABELS[s],
          total: filteredOccurrences.filter((o) => o.severity === s).length,
          color: SEVERITY_COLORS[s],
        })).filter((d) => d.total > 0);
        return (
          <div className="rounded-xl border border-border bg-card p-4">
            <p className="mb-2 text-xs font-medium text-muted-foreground">Distribuição por gravidade</p>
            <ResponsiveContainer width="100%" height={120}>
              <BarChart data={counts} layout="vertical" margin={{ top: 0, right: 16, left: 4, bottom: 0 }}>
                <XAxis type="number" tick={{ fontSize: 11 }} stroke="var(--border)" allowDecimals={false} />
                <YAxis type="category" dataKey="gravidade" tick={{ fontSize: 11 }} stroke="var(--border)" width={52} />
                <Tooltip formatter={(v: number) => [v, "ocorrências"]} />
                <Bar dataKey="total" radius={[0, 4, 4, 0]}>
                  {counts.map((entry, i) => <Cell key={i} fill={entry.color} />)}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        );
      })()}

      <div className="flex flex-wrap gap-4 items-end">
        <div className="space-y-1.5 min-w-56">
          <Label htmlFor="occurrence-search">Buscar</Label>
          <Input
            aria-label="Buscar ocorrências"
            id="occurrence-search"
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Animal, descrição, fonte"
            value={search}
          />
        </div>
        <div className="space-y-1.5 min-w-44">
          <Label htmlFor="occurrence-animal">Filtro de animal</Label>
          <select
            aria-label="Filtro de animal"
            className={selectClass}
            disabled={loadingAnimals}
            id="occurrence-animal"
            onChange={(e) => setSelectedAnimalId(e.target.value)}
            value={selectedAnimalId}
          >
            <option value="">Todos os animais</option>
            {animals.map((animal) => (
              <option key={animal.id} value={animal.id}>{formatAnimalOption(animal)}</option>
            ))}
          </select>
        </div>
        <div className="space-y-1.5 min-w-36">
          <Label htmlFor="occurrence-status">Filtro de status</Label>
          <select
            aria-label="Filtro de status"
            className={selectClass}
            id="occurrence-status"
            onChange={(e) => setStatusFilter(e.target.value as OccurrenceStatusFilter)}
            value={statusFilter}
          >
            <option value="all">Todos os status</option>
            <option value="open">Aberta</option>
            <option value="in_follow_up">Em acompanhamento</option>
            <option value="resolved">Resolvida</option>
          </select>
        </div>
        <div className="space-y-1.5 min-w-36">
          <Label htmlFor="occurrence-severity">Filtro de gravidade</Label>
          <select
            aria-label="Filtro de gravidade"
            className={selectClass}
            id="occurrence-severity"
            onChange={(e) => setSeverityFilter(e.target.value as OccurrenceSeverityFilter)}
            value={severityFilter}
          >
            <option value="all">Todas as gravidades</option>
            <option value="low">Baixa</option>
            <option value="medium">Média</option>
            <option value="high">Alta</option>
            <option value="critical">Crítica</option>
          </select>
        </div>
      </div>

      <div className="overflow-hidden rounded-lg border border-border bg-card">
        {loadingOccurrences || loadingAnimals ? (
          <div className="p-4 space-y-2" data-testid="occurrences-skeleton">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-3/4" />
          </div>
        ) : occurrencesError != null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">{occurrencesError}</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Animal</TableHead>
                <TableHead>Tipo</TableHead>
                <TableHead>Gravidade</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Descrição</TableHead>
                <TableHead>Fonte</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredOccurrences.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7}>
                    <Empty>
                      <EmptyMedia>
                        <FirstAid size={80} weight="duotone" className="text-muted-foreground" />
                      </EmptyMedia>
                      <EmptyTitle>Nenhuma ocorrência encontrada</EmptyTitle>
                      <EmptyDescription>Nenhuma ocorrência corresponde aos filtros aplicados.</EmptyDescription>
                    </Empty>
                  </TableCell>
                </TableRow>
              ) : (
                filteredOccurrences.map((occurrence) => (
                  <TableRow
                    className={`cursor-pointer ${occurrence.id === selectedOccurrence?.id ? "bg-muted/50" : ""}`}
                    key={occurrence.id}
                    onClick={() => setSelectedOccurrenceId(occurrence.id)}
                  >
                    <TableCell className="pl-4">{describeOccurrenceContext(occurrence)}</TableCell>
                    <TableCell>{capitalize(occurrence.type)}</TableCell>
                    <TableCell>
                      <SeverityBadge severity={occurrence.severity} />
                    </TableCell>
                    <TableCell>
                      <OccurrenceStatusBadge status={occurrence.status} />
                    </TableCell>
                    <TableCell className="max-w-xs truncate">{occurrence.description}</TableCell>
                    <TableCell className="text-muted-foreground">{formatOccurrenceSource(occurrence.source)}</TableCell>
                    <TableCell onClick={(e) => e.stopPropagation()}>
                      {canResolveOccurrences && (
                        <Button
                          aria-label="Excluir ocorrência"
                          onClick={() => setDeleteTarget({ id: occurrence.id, label: describeOccurrenceContext(occurrence) })}
                          size="icon"
                          type="button"
                          variant="ghost"
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {selectedOccurrence != null && (
        <div className="overflow-hidden rounded-lg border border-border bg-card p-6 space-y-4">
          <div className="flex items-start justify-between gap-4 flex-wrap">
            <div>
              <h2 className="font-heading text-base font-semibold">{capitalize(selectedOccurrence.type)}</h2>
              <p className="text-sm text-muted-foreground">{selectedOccurrence.description}</p>
            </div>
            <div className="flex gap-2">
              <SeverityBadge severity={selectedOccurrence.severity} />
              <OccurrenceStatusBadge status={selectedOccurrence.status} />
            </div>
          </div>

          <dl className="grid grid-cols-2 gap-2 text-sm sm:grid-cols-3">
            <div><dt className="text-muted-foreground">Animal</dt><dd>{describeOccurrenceContext(selectedOccurrence)}</dd></div>
            <div><dt className="text-muted-foreground">Criado</dt><dd>Criado em {formatTimestamp(selectedOccurrence.createdAt)}</dd></div>
            <div><dt className="text-muted-foreground">Atualizado</dt><dd>Atualizado em {formatTimestamp(selectedOccurrence.updatedAt)}</dd></div>
            <div><dt className="text-muted-foreground">Fonte</dt><dd>{formatOccurrenceSource(selectedOccurrence.source)}</dd></div>
            <div>
              <dt className="text-muted-foreground">Resolvido por</dt>
              <dd>{selectedOccurrence.resolvedBy != null ? `Resolvido por ${selectedOccurrence.resolvedBy}` : "Sem resolutor registrado"}</dd>
            </div>
          </dl>

          <div className="flex items-center gap-3 flex-wrap">
            {canResolveOccurrences && selectedOccurrence.status !== "resolved" && (
              <Button
                disabled={submitting}
                onClick={() => { void runOccurrenceAction("resolve"); }}
                type="button"
              >
                Resolver ocorrência
              </Button>
            )}
            {canResolveOccurrences && selectedOccurrence.status === "resolved" && (
              <Button
                disabled={submitting}
                onClick={() => { void runOccurrenceAction("reopen"); }}
                type="button"
                variant="outline"
              >
                Reabrir ocorrência
              </Button>
            )}
            {!canResolveOccurrences && (
              <span className="text-sm text-muted-foreground">
                Resolver e reabrir são ações reservadas para veterinários e administradores.
              </span>
            )}
            {actionStatus != null && (
              <span className="text-sm text-muted-foreground">{actionStatus}</span>
            )}
          </div>
        </div>
      )}
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

function describeOccurrenceContext(occurrence: OccurrenceRecord) {
  return `Animal ${occurrence.animalIdentifier} · ${occurrence.animalName ?? "Sem nome"}`;
}

function formatAnimalOption(animal: AnimalOption) {
  return `${animal.identifier} · ${animal.name ?? "Sem nome"} · ${capitalize(animal.status)}`;
}

function formatOccurrenceSeverity(value: string) {
  switch (value) {
    case "low": return "Baixa";
    case "medium": return "Média";
    case "high": return "Alta";
    case "critical": return "Crítica";
    default: return value.charAt(0).toUpperCase() + value.slice(1);
  }
}

function formatTimestamp(value: string) {
  return value.replace("T", " ").replace(/:00(?:\.\d+)?(?:Z|[+-]\d\d:\d\d)?$/, "");
}

function SeverityBadge({ severity }: { severity: string }) {
  const cfg = SEVERITY_CONFIG[severity];
  if (cfg == null) return null;
  return (
    <Badge className={cfg.classes} variant="outline">
      <cfg.Icon aria-hidden="true" />
      {cfg.label}
    </Badge>
  );
}

function OccurrenceStatusBadge({ status }: { status: string }) {
  const cfg = STATUS_CONFIG[status];
  if (cfg == null) return null;
  return (
    <Badge className={cfg.classes} variant="outline">
      <cfg.Icon aria-hidden="true" />
      {cfg.label}
    </Badge>
  );
}

