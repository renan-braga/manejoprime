import { useEffect, useState } from "react";
import { Barn } from "@phosphor-icons/react";
import { Upload } from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError } from "../../lib/apiErrors";
import { formatLotStatus } from "../../lib/formatters";
import { ImportSheet } from "../../features/onboarding/ImportSheet";
import { Badge } from "../../components/ui/badge";
import { Button } from "../../components/ui/button";
import { BillingGatedButton } from "../../components/BillingGatedButton";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "../../components/ui/table";
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
import { Empty, EmptyMedia, EmptyTitle, EmptyDescription } from "../../components/ui/empty";

type LotItem = {
  id: string;
  name: string;
  animalsCount: number;
  status: string;
};

type LotListResponse = {
  items: LotItem[];
  total: number;
  limit: number;
  offset: number;
};

const STATUS_BADGE: Record<string, string> = {
  active: "bg-emerald-100 text-emerald-700 border-emerald-200",
  archived: "bg-gray-100 text-gray-600 border-gray-200"
};

export function LotsPage() {
  const { fetchProtectedJson, session } = useAuth();
  const canWrite = session?.permissions.includes("lots.write") ?? false;

  const [lots, setLots] = useState<LotListResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState("");
  const [createSubmitting, setCreateSubmitting] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [importOpen, setImportOpen] = useState(false);

  const [archiveLot, setArchiveLot] = useState<LotItem | null>(null);
  const [archiveSubmitting, setArchiveSubmitting] = useState(false);

  useEffect(() => {
    let cancelled = false;

    async function load() {
      try {
        const data = await fetchProtectedJson<LotListResponse>("/api/v1/lots?limit=20&offset=0");
        if (!cancelled) { setLots(data); setError(null); }
      } catch (e) {
        if (!cancelled) setError(describeApiError(e, "Unexpected error."));
      }
    }

    void load();
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    if (createName.trim().length === 0) {
      setCreateError("Nome do lote é obrigatório.");
      return;
    }
    setCreateSubmitting(true);
    setCreateError(null);
    try {
      const created = await fetchProtectedJson<LotItem>("/api/v1/lots", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: createName.trim() })
      });
      setLots((prev) =>
        prev == null
          ? { items: [created], total: 1, limit: 20, offset: 0 }
          : { ...prev, items: [...prev.items, created], total: prev.total + 1 }
      );
      setCreateOpen(false);
      setCreateName("");
    } catch (e) {
      setCreateError(describeApiError(e, "Unexpected error."));
    } finally {
      setCreateSubmitting(false);
    }
  }

  async function handleArchiveConfirm() {
    if (archiveLot == null) return;
    setArchiveSubmitting(true);
    try {
      const updated = await fetchProtectedJson<LotItem>(`/api/v1/lots/${archiveLot.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: "archived" })
      });
      setLots((prev) =>
        prev == null
          ? prev
          : { ...prev, items: prev.items.map((l) => (l.id === updated.id ? updated : l)) }
      );
      setArchiveLot(null);
    } catch {
      // keep dialog open so user can retry
    } finally {
      setArchiveSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Lotes</h1>
        {canWrite ? (
          <div className="flex gap-2">
            <BillingGatedButton onClick={() => setImportOpen(true)} size="sm" type="button" variant="outline">
              <Upload className="h-4 w-4" />
              Importar planilha
            </BillingGatedButton>
            <BillingGatedButton onClick={() => setCreateOpen(true)} size="sm" type="button">
              Novo lote
            </BillingGatedButton>
          </div>
        ) : null}
      </div>

      <p className="text-sm text-muted-foreground">
        {lots != null ? `${lots.total} lotes encontrados` : "Carregando..."}
      </p>

      <div className="overflow-hidden rounded-xl border border-border bg-card">
        {lots == null && error == null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">Carregando lotes...</div>
        ) : error != null ? (
          <div className="p-8 text-center text-sm text-muted-foreground">{error}</div>
        ) : (
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="pl-4">Nome</TableHead>
                <TableHead>Cabeças</TableHead>
                <TableHead>Status</TableHead>
                {canWrite ? <TableHead /> : null}
              </TableRow>
            </TableHeader>
            <TableBody>
              {lots?.items.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={canWrite ? 4 : 3}>
                    <Empty>
                      <EmptyMedia>
                        <Barn size={80} weight="duotone" className="text-muted-foreground" />
                      </EmptyMedia>
                      <EmptyTitle>Nenhum lote encontrado</EmptyTitle>
                      <EmptyDescription>Crie o primeiro lote para organizar os animais da fazenda.</EmptyDescription>
                    </Empty>
                  </TableCell>
                </TableRow>
              ) : (
                lots?.items.map((lot) => (
                  <TableRow key={lot.id}>
                    <TableCell className="pl-4 font-medium">{lot.name}</TableCell>
                    <TableCell className="text-muted-foreground">{lot.animalsCount}</TableCell>
                    <TableCell>
                      <Badge className={STATUS_BADGE[lot.status] ?? ""} variant="outline">
                        {formatLotStatus(lot.status)}
                      </Badge>
                    </TableCell>
                    {canWrite ? (
                      <TableCell className="text-right">
                        {lot.status !== "archived" ? (
                          <Button
                            aria-label={`Archive ${lot.name}`}
                            onClick={() => setArchiveLot(lot)}
                            size="sm"
                            type="button"
                            variant="outline"
                          >
                            Arquivar
                          </Button>
                        ) : null}
                      </TableCell>
                    ) : null}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        )}
      </div>

      {/* Create dialog */}
      <Dialog onOpenChange={setCreateOpen} open={createOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Novo lote</DialogTitle>
          </DialogHeader>
          <form id="create-lot-form" onSubmit={(e) => void handleCreate(e)}>
            <div className="space-y-4 py-2">
              <div className="space-y-1.5">
                <Label htmlFor="lot-name">Nome do lote</Label>
                <Input
                  id="lot-name"
                  onChange={(e) => setCreateName(e.target.value)}
                  placeholder="ex: Engorda 1"
                  value={createName}
                />
              </div>
              {createError != null ? (
                <p className="text-sm text-destructive">{createError}</p>
              ) : null}
            </div>
          </form>
          <DialogFooter>
            <Button
              disabled={createSubmitting}
              form="create-lot-form"
              type="submit"
            >
              Criar lote
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Import sheet */}
      <Dialog onOpenChange={setImportOpen} open={importOpen}>
        <DialogContent className="max-w-3xl">
          <DialogHeader>
            <DialogTitle>Importar lotes via planilha</DialogTitle>
          </DialogHeader>
          <ImportSheet
            dataType="lots"
            onImported={() => {
              setImportOpen(false);
              void (async () => {
                try {
                  const data = await fetchProtectedJson<LotListResponse>("/api/v1/lots?limit=20&offset=0");
                  setLots(data);
                } catch { /* ignore */ }
              })();
            }}
          />
        </DialogContent>
      </Dialog>

      {/* Archive confirmation */}
      <AlertDialog onOpenChange={(open) => { if (!open) setArchiveLot(null); }} open={archiveLot != null}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>Arquivar {archiveLot?.name}?</AlertDialogTitle>
            <AlertDialogDescription>
              Arquivar remove o lote das listas ativas. Os animais neste lote precisarão ser reatribuídos.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>Cancelar</AlertDialogCancel>
            <AlertDialogAction
              disabled={archiveSubmitting}
              onClick={() => void handleArchiveConfirm()}
            >
              Arquivar
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

