import { useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, Pencil, Scale, AlertTriangle, Syringe, Pill, Trash2 } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError } from "../../lib/apiErrors";
import { capitalize, formatSexWithSymbol } from "../../lib/formatters";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { Card, CardContent } from "../../components/ui/card";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { DatePickerInput } from "../../components/ui/date-picker";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter
} from "../../components/ui/dialog";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle
} from "../../components/ui/alert-dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue
} from "../../components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../../components/ui/tabs";

// ── Types ────────────────────────────────────────────────────────────────────

type AnimalDetailResponse = {
  id: string;
  identifier: string;
  name: string | null;
  sex: string;
  breed: string | null;
  status: string;
  birthDate: string | null;
  entryDate: string | null;
  farmId: string;
  lot: { id: string; name: string } | null;
  summary: { birthWeightKg: number | null; ageInDays: number | null; daysOnFarm: number | null };
};

type WeighingItem = {
  id: string;
  animalId: string;
  weightKg: number;
  weighingDate: string;
  type: string;
  source: string;
  recordedBy: string;
  gmdKgPerDay: number | null;
};

type WeighingListResponse = { items: WeighingItem[]; total: number; limit: number; offset: number };

type OccurrenceItem = {
  id: string;
  animalId: string;
  type: string;
  severity: string;
  status: string;
  description: string;
  source: string;
  createdAt: string;
};

type OccurrenceListResponse = { items: OccurrenceItem[]; total: number; limit: number; offset: number };

type VaccineItem = {
  id: string;
  animalId: string;
  vaccineType: string;
  vaccineBatch: string;
  dose: string;
  applicationDate: string;
  nextDoseDate: string | null;
  volumeMl: number | null;
  source: string;
};

type VaccineListResponse = { items: VaccineItem[]; total: number; limit: number; offset: number };

type MedicationItem = {
  id: string;
  animalId: string;
  medicationName: string;
  activeIngredient: string | null;
  reason: string | null;
  dosage: string | null;
  withdrawalPeriodDays: number;
  applicationDate: string;
  releaseDate: string;
  inWithdrawalPeriod: boolean;
  cost: number | null;
  source: string;
};

type MedicationListResponse = { items: MedicationItem[]; total: number; limit: number; offset: number };

// ── Status maps ───────────────────────────────────────────────────────────────

const STATUS_BADGE: Record<string, string> = {
  active: "bg-emerald-100 text-emerald-700 border-emerald-200",
  sold: "bg-blue-100 text-blue-700 border-blue-200",
  dead: "bg-gray-100 text-gray-600 border-gray-200",
  transferred: "bg-amber-100 text-amber-700 border-amber-200"
};

const STATUS_LABEL: Record<string, string> = {
  active: "Ativo",
  sold: "Vendido",
  dead: "Morto",
  transferred: "Transferido"
};

const SEVERITY_BADGE: Record<string, string> = {
  low: "bg-yellow-100 text-yellow-700 border-yellow-200",
  medium: "bg-orange-100 text-orange-700 border-orange-200",
  high: "bg-red-100 text-red-700 border-red-200",
  critical: "bg-red-200 text-red-800 border-red-300"
};

const OCC_STATUS_BADGE: Record<string, string> = {
  open: "bg-amber-100 text-amber-700 border-amber-200",
  resolved: "bg-emerald-100 text-emerald-700 border-emerald-200"
};

// ── Component ─────────────────────────────────────────────────────────────────

type EditForm = {
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

type LotOption = { id: string; name: string };

export function AnimalDetailPage() {
  const { fetchProtectedJson, session } = useAuth();
  const { animalId } = useParams<{ animalId: string }>();
  const navigate = useNavigate();

  const [animal, setAnimal] = useState<AnimalDetailResponse | null>(null);
  const [weighings, setWeighings] = useState<WeighingListResponse | null>(null);
  const [occurrences, setOccurrences] = useState<OccurrenceListResponse | null>(null);
  const [vaccines, setVaccines] = useState<VaccineListResponse | null>(null);
  const [medications, setMedications] = useState<MedicationListResponse | null>(null);
  const [animalError, setAnimalError] = useState<string | null>(null);

  const [lots, setLots] = useState<LotOption[]>([]);

  const [editOpen, setEditOpen] = useState(false);
  const [editForm, setEditForm] = useState<EditForm>({ identifier: "", name: "", sex: "male", breed: "", status: "active", birthDate: "", entryDate: "", birthWeightKg: "", lotId: "" });
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editError, setEditError] = useState<string | null>(null);

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteSubmitting, setDeleteSubmitting] = useState(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const canWrite = session?.permissions.includes("animals.write") ?? false;

  useEffect(() => {
    if (!canWrite) return;
    let cancelled = false;
    fetchProtectedJson<{ items: LotOption[] }>("/api/v1/lots")
      .then((d) => { if (!cancelled) setLots(d.items); })
      .catch(() => {});
    return () => { cancelled = true; };
  }, [canWrite, fetchProtectedJson]);

  useEffect(() => {
    if (!animalId) return;
    let cancelled = false;
    fetchProtectedJson<AnimalDetailResponse>(`/api/v1/animals/${animalId}`)
      .then((d) => { if (!cancelled) setAnimal(d); })
      .catch((e) => { if (!cancelled) setAnimalError(describeApiError(e, "Failed to load animal.")); });
    return () => { cancelled = true; };
  }, [animalId, fetchProtectedJson]);

  useEffect(() => {
    if (!animalId) return;
    let cancelled = false;
    fetchProtectedJson<WeighingListResponse>(`/api/v1/weighings?animalId=${animalId}&limit=20`)
      .then((d) => { if (!cancelled) setWeighings(d); })
      .catch(() => { if (!cancelled) setWeighings({ items: [], total: 0, limit: 20, offset: 0 }); });
    return () => { cancelled = true; };
  }, [animalId, fetchProtectedJson]);

  useEffect(() => {
    if (!animalId) return;
    let cancelled = false;
    fetchProtectedJson<OccurrenceListResponse>(`/api/v1/occurrences?animalId=${animalId}&limit=20`)
      .then((d) => { if (!cancelled) setOccurrences(d); })
      .catch(() => { if (!cancelled) setOccurrences({ items: [], total: 0, limit: 20, offset: 0 }); });
    return () => { cancelled = true; };
  }, [animalId, fetchProtectedJson]);

  useEffect(() => {
    if (!animalId) return;
    let cancelled = false;
    fetchProtectedJson<VaccineListResponse>(`/api/v1/vaccines?animalId=${animalId}&limit=20`)
      .then((d) => { if (!cancelled) setVaccines(d); })
      .catch(() => { if (!cancelled) setVaccines({ items: [], total: 0, limit: 20, offset: 0 }); });
    return () => { cancelled = true; };
  }, [animalId, fetchProtectedJson]);

  useEffect(() => {
    if (!animalId) return;
    let cancelled = false;
    fetchProtectedJson<MedicationListResponse>(`/api/v1/medications?animalId=${animalId}&limit=20`)
      .then((d) => { if (!cancelled) setMedications(d); })
      .catch(() => { if (!cancelled) setMedications({ items: [], total: 0, limit: 20, offset: 0 }); });
    return () => { cancelled = true; };
  }, [animalId, fetchProtectedJson]);

  const latestWeight = weighings?.items[0]?.weightKg ?? null;

  async function handleEdit(e: React.FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (editForm.identifier.trim().length === 0) {
      setEditError("O brinco (identificador) é obrigatório.");
      return;
    }
    setEditSubmitting(true);
    setEditError(null);
    const body: Record<string, string> = {
      identifier: editForm.identifier.trim(),
      sex: editForm.sex,
      status: editForm.status
    };
    if (editForm.name.trim().length > 0) body.name = editForm.name.trim();
    if (editForm.breed.trim().length > 0) body.breed = editForm.breed.trim();
    if (editForm.birthDate.trim().length > 0) body.birthDate = editForm.birthDate.trim();
    if (editForm.entryDate.trim().length > 0) body.entryDate = editForm.entryDate.trim();
    if (editForm.birthWeightKg.trim().length > 0) body.birthWeightKg = editForm.birthWeightKg.trim();
    if (editForm.lotId.length > 0) body.lotId = editForm.lotId;
    try {
      await fetchProtectedJson(`/api/v1/animals/${animalId}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
      });
      setEditOpen(false);
      const refreshed = await fetchProtectedJson<AnimalDetailResponse>(`/api/v1/animals/${animalId}`);
      setAnimal(refreshed);
    } catch (error) {
      setEditError(describeApiError(error));
    } finally {
      setEditSubmitting(false);
    }
  }

  async function handleDelete() {
    setDeleteSubmitting(true);
    setDeleteError(null);
    try {
      await fetchProtectedJson(`/api/v1/animals/${animalId}`, { method: "DELETE" });
      navigate("/app/animals");
    } catch (error) {
      setDeleteSubmitting(false);
      setDeleteOpen(false);
      setDeleteError(describeApiError(error));
    }
  }

  return (
    <div className="space-y-6">
      {/* Back */}
      <Link
        className="inline-flex items-center gap-1.5 text-sm text-muted-foreground hover:text-foreground"
        to="/app/animals"
      >
        <ArrowLeft className="h-4 w-4" /> Voltar para Animais
      </Link>

      {deleteError != null ? (
        <p className="text-sm text-destructive">{deleteError}</p>
      ) : null}

      {/* Edit dialog */}
      <Dialog onOpenChange={(open) => { setEditOpen(open); if (!open) setEditError(null); }} open={editOpen}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>Editar animal</DialogTitle>
          </DialogHeader>
          <form className="space-y-4" onSubmit={(e) => void handleEdit(e)}>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label htmlFor="edit-identifier">Brinco *</Label>
                <Input id="edit-identifier" onChange={(e) => setEditForm((f) => ({ ...f, identifier: e.target.value }))} value={editForm.identifier} />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="edit-name">Nome</Label>
                <Input id="edit-name" onChange={(e) => setEditForm((f) => ({ ...f, name: e.target.value }))} placeholder="Ex: Trovão" value={editForm.name} />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-1.5">
                <Label>Sexo *</Label>
                <Select onValueChange={(v) => setEditForm((f) => ({ ...f, sex: v }))} value={editForm.sex}>
                  <SelectTrigger aria-label="Select sex"><SelectValue /></SelectTrigger>
                  <SelectContent>
                    <SelectItem value="male">Macho</SelectItem>
                    <SelectItem value="female">Fêmea</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1.5">
                <Label>Status</Label>
                <Select onValueChange={(v) => setEditForm((f) => ({ ...f, status: v }))} value={editForm.status}>
                  <SelectTrigger aria-label="Select status"><SelectValue /></SelectTrigger>
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
                <Label htmlFor="edit-breed">Raça</Label>
                <Input id="edit-breed" onChange={(e) => setEditForm((f) => ({ ...f, breed: e.target.value }))} placeholder="Ex: Nelore" value={editForm.breed} />
              </div>
              <div className="space-y-1.5">
                <Label>Lote</Label>
                <Select
                  onValueChange={(v) => setEditForm((f) => ({ ...f, lotId: v === "_none" ? "" : v }))}
                  value={editForm.lotId === "" ? "_none" : editForm.lotId}
                >
                  <SelectTrigger aria-label="Select lot"><SelectValue placeholder="Sem lote" /></SelectTrigger>
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
                <Label htmlFor="edit-birth-date">Nascimento</Label>
                <DatePickerInput
                  id="edit-birth-date"
                  onChange={(v) => setEditForm((f) => ({ ...f, birthDate: v }))}
                  placeholder="Selecionar data"
                  value={editForm.birthDate}
                />
              </div>
              <div className="space-y-1.5">
                <Label htmlFor="edit-entry-date">Entrada</Label>
                <DatePickerInput
                  id="edit-entry-date"
                  onChange={(v) => setEditForm((f) => ({ ...f, entryDate: v }))}
                  placeholder="Selecionar data"
                  value={editForm.entryDate}
                />
              </div>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="edit-birth-weight">Peso ao nascer (kg)</Label>
              <Input id="edit-birth-weight" inputMode="decimal" onChange={(e) => setEditForm((f) => ({ ...f, birthWeightKg: e.target.value }))} placeholder="Ex: 32.5" value={editForm.birthWeightKg} />
            </div>
            {editError != null ? <p className="text-sm text-destructive">{editError}</p> : null}
            <DialogFooter>
              <Button onClick={() => setEditOpen(false)} type="button" variant="outline">Cancelar</Button>
              <Button disabled={editSubmitting} type="submit">{editSubmitting ? "Salvando..." : "Salvar"}</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation dialog */}
      <AlertDialog onOpenChange={setDeleteOpen} open={deleteOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Excluir animal?</AlertDialogTitle>
            <AlertDialogDescription>
              Esta ação é irreversível. O animal será removido permanentemente do sistema.
              Animais com pesagens, vacinas ou outras ocorrências registradas não podem ser excluídos.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteSubmitting}
              onClick={() => void handleDelete()}
            >
              {deleteSubmitting ? "Excluindo..." : "Excluir"}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {/* Header */}
      {animalError != null ? (
        <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">{animalError}</div>
      ) : animal == null ? (
        <div className="rounded-xl border border-border bg-card p-6 text-sm text-muted-foreground">Carregando animal...</div>
      ) : (
        <div className="rounded-xl border border-border bg-card p-6 space-y-4">
          <div className="flex items-start justify-between gap-4">
            {/* Photo placeholder */}
            <div className="flex items-center gap-4">
              <div className="h-16 w-16 rounded-lg bg-muted flex items-center justify-center shrink-0 text-2xl">
                🐄
              </div>
              <div>
                <div className="flex items-center gap-2 flex-wrap">
                  <h1 className="font-heading text-2xl font-bold">
                    <span className="text-muted-foreground font-mono text-lg">#{animal.identifier}</span>{" "}
                    {animal.name ?? `Animal ${animal.identifier}`}
                  </h1>
                  <Badge className={STATUS_BADGE[animal.status] ?? ""} variant="outline">
                    {STATUS_LABEL[animal.status] ?? animal.status}
                  </Badge>
                </div>
                <p className="text-sm text-muted-foreground mt-0.5">
                  {animal.breed ?? "Raça desconhecida"} · {formatSexWithSymbol(animal.sex)}
                  {animal.birthDate != null ? ` · Nascido em ${animal.birthDate}` : ""}
                </p>
              </div>
            </div>
            {canWrite ? (
              <div className="flex gap-2 shrink-0">
                <Button
                  onClick={() => {
                    setEditForm({
                      identifier: animal.identifier,
                      name: animal.name ?? "",
                      sex: animal.sex,
                      breed: animal.breed ?? "",
                      status: animal.status,
                      birthDate: animal.birthDate ?? "",
                      entryDate: animal.entryDate ?? "",
                      birthWeightKg: animal.summary.birthWeightKg != null ? String(animal.summary.birthWeightKg) : "",
                      lotId: animal.lot?.id ?? ""
                    });
                    setEditOpen(true);
                  }}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  <Pencil className="h-4 w-4" />
                  Editar
                </Button>
                <Button
                  className="text-destructive hover:text-destructive"
                  onClick={() => setDeleteOpen(true)}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  <Trash2 className="h-4 w-4" />
                  Excluir
                </Button>
              </div>
            ) : null}
          </div>

          {/* KPI row */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <KpiCard label="Lote" value={animal.lot?.name ?? "—"} />
            <KpiCard label="Último peso" value={latestWeight != null ? `${latestWeight} kg` : "—"} />
            <KpiCard label="Dias na fazenda" value={animal.summary.daysOnFarm != null ? `${animal.summary.daysOnFarm}d` : "—"} />
            <KpiCard label="Idade" value={animal.summary.ageInDays != null ? `${animal.summary.ageInDays}d` : "—"} />
          </div>
        </div>
      )}

      {/* Tabs */}
      <Tabs defaultValue="weighings">
        <TabsList>
          <TabsTrigger value="weighings">
            <Scale className="h-3.5 w-3.5" />
            Pesagens
            {weighings != null ? <CountBadge n={weighings.total} /> : null}
          </TabsTrigger>
          <TabsTrigger value="occurrences">
            <AlertTriangle className="h-3.5 w-3.5" />
            Ocorrências
            {occurrences != null ? <CountBadge n={occurrences.total} /> : null}
          </TabsTrigger>
          <TabsTrigger value="vaccines">
            <Syringe className="h-3.5 w-3.5" />
            Vacinas
            {vaccines != null ? <CountBadge n={vaccines.total} /> : null}
          </TabsTrigger>
          <TabsTrigger value="medications">
            <Pill className="h-3.5 w-3.5" />
            Medicamentos
            {medications != null ? <CountBadge n={medications.total} /> : null}
          </TabsTrigger>
        </TabsList>

        {/* Weighings */}
        <TabsContent value="weighings" className="mt-4 space-y-2">
          {weighings == null ? (
            <TabSkeleton />
          ) : weighings.items.length === 0 ? (
            <TabEmpty message="Nenhuma pesagem registrada." />
          ) : (
            weighings.items.map((w) => (
              <Card key={w.id} className="border-border">
                <CardContent className="p-4 flex items-center gap-4">
                  <Scale className="h-5 w-5 text-emerald-600 shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-3 flex-wrap">
                      <span className="font-semibold">{w.weightKg} kg</span>
                      {w.gmdKgPerDay != null ? (
                        <span className="text-sm text-muted-foreground">{w.gmdKgPerDay} kg/dia</span>
                      ) : null}
                      <SourceBadge source={w.source} />
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">{w.weighingDate}</p>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>

        {/* Occurrences */}
        <TabsContent value="occurrences" className="mt-4 space-y-2">
          {occurrences == null ? (
            <TabSkeleton />
          ) : occurrences.items.length === 0 ? (
            <TabEmpty message="Nenhuma ocorrência registrada." />
          ) : (
            occurrences.items.map((o) => (
              <Card key={o.id} className="border-border">
                <CardContent className="p-4 flex items-start gap-4">
                  <AlertTriangle className="h-5 w-5 text-amber-500 shrink-0 mt-0.5" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Badge className={SEVERITY_BADGE[o.severity] ?? ""} variant="outline">
                        {capitalize(o.severity)}
                      </Badge>
                      <Badge className={OCC_STATUS_BADGE[o.status] ?? ""} variant="outline">
                        {capitalize(o.status)}
                      </Badge>
                      <SourceBadge source={o.source} />
                    </div>
                    <p className="text-sm mt-1">{o.description}</p>
                    <p className="text-xs text-muted-foreground mt-0.5">{formatDate(o.createdAt)}</p>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>

        {/* Vaccines */}
        <TabsContent value="vaccines" className="mt-4 space-y-2">
          {vaccines == null ? (
            <TabSkeleton />
          ) : vaccines.items.length === 0 ? (
            <TabEmpty message="Nenhuma vacina registrada." />
          ) : (
            vaccines.items.map((v) => (
              <Card key={v.id} className="border-border">
                <CardContent className="p-4 flex items-center gap-4">
                  <Syringe className="h-5 w-5 text-blue-600 shrink-0" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold">{v.vaccineType}</span>
                      <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                        {v.dose}
                      </Badge>
                      <SourceBadge source={v.source} />
                    </div>
                    <p className="text-xs text-muted-foreground mt-0.5">
                      Lote {v.vaccineBatch} · Aplicado em {v.applicationDate}
                      {v.nextDoseDate != null ? ` · Próxima dose ${v.nextDoseDate}` : ""}
                    </p>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>

        {/* Medications */}
        <TabsContent value="medications" className="mt-4 space-y-2">
          {medications == null ? (
            <TabSkeleton />
          ) : medications.items.length === 0 ? (
            <TabEmpty message="Nenhum medicamento registrado." />
          ) : (
            medications.items.map((m) => (
              <Card key={m.id} className="border-border">
                <CardContent className="p-4 flex items-start gap-4">
                  <Pill className="h-5 w-5 text-violet-600 shrink-0 mt-0.5" />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold">{m.medicationName}</span>
                      {m.inWithdrawalPeriod ? (
                        <Badge variant="outline" className="bg-red-100 text-red-700 border-red-200">
                          Em carência
                        </Badge>
                      ) : null}
                      <SourceBadge source={m.source} />
                    </div>
                    {m.reason != null ? <p className="text-sm text-muted-foreground mt-0.5">{m.reason}</p> : null}
                    <p className="text-xs text-muted-foreground mt-0.5">
                      {m.dosage != null ? `${m.dosage} · ` : ""}
                      Aplicado em {m.applicationDate} · Carência até {m.releaseDate}
                    </p>
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ── Sub-components ─────────────────────────────────────────────────────────────

function KpiCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-border bg-muted/30 p-3">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="font-heading text-base font-semibold mt-0.5">{value}</p>
    </div>
  );
}

function CountBadge({ n }: { n: number }) {
  return (
    <span className="ml-0.5 rounded-full bg-muted px-1.5 py-0.5 text-[10px] text-muted-foreground">{n}</span>
  );
}

function SourceBadge({ source }: { source: string }) {
  const isWhatsApp = source === "whatsapp";
  return (
    <span className={`text-[10px] px-1.5 py-0.5 rounded-full border ${isWhatsApp ? "bg-green-50 text-green-700 border-green-200" : "bg-muted text-muted-foreground border-border"}`}>
      {isWhatsApp ? "WhatsApp" : "Manual"}
    </span>
  );
}

function TabSkeleton() {
  return (
    <div className="space-y-2">
      {[1, 2, 3].map((i) => (
        <div key={i} className="h-16 rounded-lg border border-border bg-muted/30 animate-pulse" />
      ))}
    </div>
  );
}

function TabEmpty({ message }: { message: string }) {
  return <p className="py-8 text-center text-sm text-muted-foreground">{message}</p>;
}

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatDate(iso: string) {
  try {
    return new Date(iso).toLocaleDateString("pt-BR", { year: "numeric", month: "short", day: "numeric" });
  } catch {
    return iso;
  }
}

