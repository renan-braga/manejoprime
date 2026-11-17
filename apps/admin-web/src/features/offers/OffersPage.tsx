import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { usePageTour } from "@/hooks/usePageTour";
import { AppTour } from "../shell/AppTour";
import type { Step } from "react-joyride";

const OFFERS_TOUR_STEPS: Step[] = [
  {
    target: "[data-tour='offers-add']",
    title: "Nova oferta",
    content: "Crie uma oferta selecionando os animais que deseja vender e definindo preço e validade.",
    placement: "bottom",
  },
  {
    target: "[data-tour='offers-tabs']",
    title: "Filtrar por status",
    content: "Navegue entre rascunhos, ofertas ativas, vendidas e canceladas com um clique.",
    placement: "bottom",
  },
];
import { Tag as TagPhosphor } from "@phosphor-icons/react";
import { describeApiError, readFieldErrors } from "../../lib/apiErrors";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { Checkbox } from "../../components/ui/checkbox";
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
import { Tabs, TabsList, TabsTrigger } from "../../components/ui/tabs";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "../../components/ui/table";
import { Empty, EmptyMedia, EmptyTitle, EmptyDescription } from "../../components/ui/empty";

type AnimalOption = {
  id: string;
  identifier: string;
  name: string | null;
};

type OfferListItem = {
  id: string;
  farmId: string;
  title: string;
  askingPrice: number;
  buyerContact: string | null;
  expiresAt: string | null;
  status: string;
  animalCount: number;
  createdAt: string;
  updatedAt: string;
};

type OfferListResponse = { items: OfferListItem[] };

type AnimalListResponse = {
  items: AnimalOption[];
  total: number;
  limit: number;
  offset: number;
};

type WizardStep = 1 | 2;

type OfferFormState = {
  title: string;
  notes: string;
  askingPrice: string;
  buyerContact: string;
  expiresAt: string;
};

const DEFAULT_FORM: OfferFormState = {
  title: "",
  notes: "",
  askingPrice: "",
  buyerContact: "",
  expiresAt: "",
};

const STATUS_TABS = [
  { value: "all", label: "Todas" },
  { value: "draft", label: "Rascunho" },
  { value: "active", label: "Ativa" },
  { value: "sold", label: "Vendida" },
  { value: "cancelled", label: "Cancelada" },
];

export function OffersPage() {
  const { fetchProtectedJson } = useAuth();
  const { run: runTour, finish: finishTour } = usePageTour("offers");
  const [offers, setOffers] = useState<OfferListItem[]>([]);
  const [animals, setAnimals] = useState<AnimalOption[]>([]);
  const [loading, setLoading] = useState(false);
  const [statusFilter, setStatusFilter] = useState("all");

  const [showWizard, setShowWizard] = useState(false);
  const [wizardStep, setWizardStep] = useState<WizardStep>(1);
  const [selectedAnimalIds, setSelectedAnimalIds] = useState<Set<string>>(new Set());
  const [form, setForm] = useState<OfferFormState>(DEFAULT_FORM);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const [markSoldOfferId, setMarkSoldOfferId] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const loadOffers = useCallback(async () => {
    setLoading(true);
    try {
      const response = await fetchProtectedJson<OfferListResponse>("/api/v1/offers");
      setOffers(response.items);
    } catch {
      // leave list as-is on error
    } finally {
      setLoading(false);
    }
  }, [fetchProtectedJson]);

  useEffect(() => {
    let cancelled = false;

    async function init() {
      setLoading(true);
      try {
        const [offersResponse, animalsResponse] = await Promise.all([
          fetchProtectedJson<OfferListResponse>("/api/v1/offers"),
          fetchProtectedJson<AnimalListResponse>("/api/v1/animals?limit=100&offset=0"),
        ]);
        if (cancelled) return;
        setOffers(offersResponse.items);
        setAnimals(animalsResponse.items);
      } catch {
        // ignore
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    void init();
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  function openWizard() {
    setForm(DEFAULT_FORM);
    setSelectedAnimalIds(new Set());
    setFieldErrors({});
    setWizardStep(1);
    setShowWizard(true);
  }

  function toggleAnimal(id: string) {
    setSelectedAnimalIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function submitOffer() {
    setSubmitting(true);
    setFieldErrors({});
    const payload: Record<string, unknown> = {
      title: form.title,
      askingPrice: form.askingPrice !== "" ? Number(form.askingPrice) : null,
      animalIds: Array.from(selectedAnimalIds),
    };
    if (form.notes !== "") payload.notes = form.notes;
    if (form.buyerContact !== "") payload.buyerContact = form.buyerContact;
    if (form.expiresAt !== "") payload.expiresAt = form.expiresAt;

    try {
      await fetchProtectedJson("/api/v1/offers", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      setShowWizard(false);
      await loadOffers();
    } catch (error) {
      setFieldErrors(readFieldErrors(error));
    } finally {
      setSubmitting(false);
    }
  }

  async function performAction(offerId: string, action: "activate" | "cancel" | "mark-sold") {
    setActionError(null);
    try {
      await fetchProtectedJson(`/api/v1/offers/${offerId}/${action}`, { method: "POST" });
      await loadOffers();
    } catch (error) {
      setActionError(describeApiError(error));
    }
  }

  const visibleOffers = statusFilter === "all" ? offers : offers.filter((o) => o.status === statusFilter);
  const draftCount = offers.filter((o) => o.status === "draft").length;
  const activeCount = offers.filter((o) => o.status === "active").length;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="font-heading text-2xl font-bold tracking-tight">Ofertas</h1>
          <p className="text-sm text-muted-foreground">
            Crie e gerencie ofertas de venda do seu rebanho.
          </p>
        </div>
        <BillingGatedButton data-tour="offers-add" onClick={openWizard} type="button">Nova oferta</BillingGatedButton>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Total de ofertas</p>
          <p className="mt-1 text-2xl font-semibold">{offers.length}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Rascunhos</p>
          <p className="mt-1 text-2xl font-semibold">{draftCount}</p>
        </div>
        <div className="rounded-lg border border-border bg-card p-4">
          <p className="text-sm text-muted-foreground">Ativas</p>
          <p className="mt-1 text-2xl font-semibold">{activeCount}</p>
        </div>
      </div>

      <Tabs data-tour="offers-tabs" onValueChange={setStatusFilter} value={statusFilter}>
        <TabsList>
          {STATUS_TABS.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value}>{tab.label}</TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {actionError != null && <p className="text-sm text-destructive">{actionError}</p>}

      <div className="overflow-hidden rounded-lg border border-border bg-card">
        {loading ? (
          <div className="space-y-2 p-4" data-testid="offers-skeleton">
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
            <Skeleton className="h-8 w-full" />
          </div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Título</TableHead>
                <TableHead>Preço pedido</TableHead>
                <TableHead>Animais</TableHead>
                <TableHead>Validade</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Ações</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {visibleOffers.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6}>
                    <Empty>
                      <EmptyMedia>
                        <TagPhosphor size={80} weight="duotone" className="text-muted-foreground" />
                      </EmptyMedia>
                      <EmptyTitle>Nenhuma oferta encontrada</EmptyTitle>
                      <EmptyDescription>Crie uma oferta selecionando os animais que deseja vender.</EmptyDescription>
                    </Empty>
                  </TableCell>
                </TableRow>
              ) : (
                visibleOffers.map((offer) => (
                  <TableRow key={offer.id}>
                    <TableCell className="pl-4 font-medium">{offer.title}</TableCell>
                    <TableCell>{formatPrice(offer.askingPrice)}</TableCell>
                    <TableCell>{offer.animalCount}</TableCell>
                    <TableCell>{offer.expiresAt ?? "—"}</TableCell>
                    <TableCell>
                      <Badge className={statusBadgeClass(offer.status)} variant="outline">
                        {formatOfferStatus(offer.status)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        {offer.status === "draft" && (
                          <>
                            <Button
                              onClick={() => { void performAction(offer.id, "activate"); }}
                              size="sm"
                              type="button"
                              variant="default"
                            >
                              Ativar
                            </Button>
                            <Button
                              onClick={() => { void performAction(offer.id, "cancel"); }}
                              size="sm"
                              type="button"
                              variant="outline"
                            >
                              Cancelar
                            </Button>
                          </>
                        )}
                        {offer.status === "active" && (
                          <>
                            <Button
                              onClick={() => setMarkSoldOfferId(offer.id)}
                              size="sm"
                              type="button"
                              variant="default"
                            >
                              Marcar vendida
                            </Button>
                            <Button
                              onClick={() => { void performAction(offer.id, "cancel"); }}
                              size="sm"
                              type="button"
                              variant="outline"
                            >
                              Cancelar
                            </Button>
                          </>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Create offer wizard */}
      <Dialog onOpenChange={setShowWizard} open={showWizard}>
        <DialogContent className="max-w-lg">
          {wizardStep === 1 && (
            <>
              <DialogHeader>
                <DialogTitle>Nova oferta — selecionar animais</DialogTitle>
              </DialogHeader>
              <div className="max-h-64 overflow-y-auto space-y-2 py-2">
                {animals.length === 0 ? (
                  <p className="text-sm text-muted-foreground">Nenhum animal disponível.</p>
                ) : (
                  animals.map((animal) => (
                    <div className="flex items-center gap-2" key={animal.id}>
                      <Checkbox
                        checked={selectedAnimalIds.has(animal.id)}
                        id={`animal-${animal.id}`}
                        onCheckedChange={() => toggleAnimal(animal.id)}
                      />
                      <Label htmlFor={`animal-${animal.id}`}>
                        {animal.identifier}{animal.name != null ? ` — ${animal.name}` : ""}
                      </Label>
                    </div>
                  ))
                )}
              </div>
              <DialogFooter>
                <Button onClick={() => setShowWizard(false)} type="button" variant="outline">Cancelar</Button>
                <Button onClick={() => setWizardStep(2)} type="button">Próximo</Button>
              </DialogFooter>
            </>
          )}

          {wizardStep === 2 && (
            <>
              <DialogHeader>
                <DialogTitle>Nova oferta — detalhes</DialogTitle>
              </DialogHeader>
              <div className="grid grid-cols-1 gap-4 py-2 sm:grid-cols-2">
                <div className="space-y-1.5 sm:col-span-2">
                  <Label htmlFor="offer-title">Título</Label>
                  <Input
                    aria-label="Título"
                    id="offer-title"
                    onChange={(e) => setForm((f) => ({ ...f, title: e.target.value }))}
                    value={form.title}
                  />
                  {fieldErrors.title != null && <p className="text-xs text-destructive">{fieldErrors.title}</p>}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="offer-price">Preço pedido</Label>
                  <Input
                    aria-label="Preço pedido"
                    id="offer-price"
                    onChange={(e) => setForm((f) => ({ ...f, askingPrice: e.target.value }))}
                    type="number"
                    value={form.askingPrice}
                  />
                  {fieldErrors.askingPrice != null && <p className="text-xs text-destructive">{fieldErrors.askingPrice}</p>}
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="offer-expires">Validade</Label>
                  <DatePickerInput
                    id="offer-expires"
                    onChange={(v) => setForm((f) => ({ ...f, expiresAt: v }))}
                    placeholder="Selecionar data (opcional)"
                    value={form.expiresAt}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="offer-buyer">Contato do comprador</Label>
                  <Input
                    aria-label="Contato do comprador"
                    id="offer-buyer"
                    onChange={(e) => setForm((f) => ({ ...f, buyerContact: e.target.value }))}
                    value={form.buyerContact}
                  />
                </div>

                <div className="space-y-1.5">
                  <Label htmlFor="offer-notes">Observações</Label>
                  <Input
                    aria-label="Observações"
                    id="offer-notes"
                    onChange={(e) => setForm((f) => ({ ...f, notes: e.target.value }))}
                    value={form.notes}
                  />
                </div>
              </div>
              <DialogFooter>
                <Button onClick={() => setWizardStep(1)} type="button" variant="outline">Voltar</Button>
                <Button disabled={submitting} onClick={() => { void submitOffer(); }} type="button">
                  Salvar oferta
                </Button>
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>

      {/* Mark sold confirmation */}
      <Dialog onOpenChange={(open) => { if (!open) setMarkSoldOfferId(null); }} open={markSoldOfferId != null}>
        <DialogContent className="max-w-sm">
          <DialogHeader>
            <DialogTitle>Marcar como vendida?</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Isso marcará a oferta como vendida e a encerrará. Esta ação não pode ser desfeita.
          </p>
          <DialogFooter>
            <Button onClick={() => setMarkSoldOfferId(null)} type="button" variant="outline">
              Cancelar
            </Button>
            <Button
              onClick={() => {
                if (markSoldOfferId != null) {
                  void performAction(markSoldOfferId, "mark-sold");
                  setMarkSoldOfferId(null);
                }
              }}
              type="button"
            >
              Confirmar
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
      <AppTour steps={OFFERS_TOUR_STEPS} run={runTour} onFinish={finishTour} />
    </div>
  );
}

function formatPrice(value: number) {
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

function formatOfferStatus(status: string) {
  switch (status) {
    case "draft": return "Rascunho";
    case "active": return "Ativa";
    case "sold": return "Vendida";
    case "cancelled": return "Cancelada";
    default: return status;
  }
}

function statusBadgeClass(status: string) {
  switch (status) {
    case "active": return "border-emerald-200 bg-emerald-100 text-emerald-700";
    case "sold": return "border-blue-200 bg-blue-100 text-blue-700";
    case "cancelled": return "border-red-200 bg-red-100 text-red-700";
    default: return "border-border bg-muted text-muted-foreground";
  }
}

