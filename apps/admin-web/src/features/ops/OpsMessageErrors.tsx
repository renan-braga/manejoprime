import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import { opsApi, OpsAuthError } from "./opsApi";
import type { OpsMessageErrorPage, OpsRecentError } from "./opsApi";

const PAGE_SIZE = 20;

function formatDateTime(iso: string): string {
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit"
  });
}

export function OpsMessageErrors({ onAuthError }: { onAuthError: () => void }) {
  const [page, setPage] = useState<OpsMessageErrorPage | null>(null);
  const [offset, setOffset] = useState(0);

  useEffect(() => {
    setPage(null);
    opsApi.messageErrors(PAGE_SIZE, offset).then(setPage).catch((e) => {
      if (e instanceof OpsAuthError) onAuthError();
    });
  }, [offset, onAuthError]);

  const totalPages = page ? Math.ceil(page.total / PAGE_SIZE) : 0;
  const currentPage = Math.floor(offset / PAGE_SIZE) + 1;

  return (
    <Card>
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <div>
          <CardTitle className="text-sm font-heading font-semibold">Erros recentes</CardTitle>
          {page && (
            <p className="text-xs text-muted-foreground mt-0.5">
              {page.total.toLocaleString("pt-BR")} mensagens com falha no total
            </p>
          )}
        </div>
        {page && totalPages > 1 && (
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <button
              onClick={() => setOffset(Math.max(0, offset - PAGE_SIZE))}
              disabled={offset === 0}
              className="px-2 py-1 rounded bg-muted hover:bg-muted/80 disabled:opacity-40"
            >
              ‹
            </button>
            <span>{currentPage} / {totalPages}</span>
            <button
              onClick={() => setOffset(offset + PAGE_SIZE)}
              disabled={offset + PAGE_SIZE >= page.total}
              className="px-2 py-1 rounded bg-muted hover:bg-muted/80 disabled:opacity-40"
            >
              ›
            </button>
          </div>
        )}
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/40">
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Recebida em</th>
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Fazenda</th>
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Usuário</th>
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Motivo da falha</th>
              </tr>
            </thead>
            <tbody>
              {!page
                ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i} className="border-b">
                      {Array.from({ length: 4 }).map((_, j) => (
                        <td key={j} className="px-4 py-2.5"><Skeleton className="h-4 w-full" /></td>
                      ))}
                    </tr>
                  ))
                : page.errors.length === 0
                ? (
                    <tr>
                      <td colSpan={4} className="px-4 py-6 text-center text-sm text-muted-foreground">
                        Nenhuma mensagem com falha.
                      </td>
                    </tr>
                  )
                : page.errors.map((error: OpsRecentError) => (
                    <tr key={error.messageId} className="border-b last:border-0">
                      <td className="px-4 py-2.5 text-muted-foreground whitespace-nowrap">{formatDateTime(error.receivedAt)}</td>
                      <td className="px-4 py-2.5 font-mono text-xs">{error.farmId.slice(0, 8)}…</td>
                      <td className="px-4 py-2.5 font-mono text-xs">{error.userId ? `${error.userId.slice(0, 8)}…` : "—"}</td>
                      <td className="px-4 py-2.5 text-red-600 text-xs">{error.failureReason ?? "—"}</td>
                    </tr>
                  ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
