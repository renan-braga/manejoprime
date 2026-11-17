import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ArrowLeft, AlertTriangle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import { getStoredOpsKey, opsApi, OpsAuthError } from "./opsApi";
import type { OpsFarmDetail } from "./opsApi";

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit"
  });
}

export function OpsFarmDetailPage() {
  const { farmId } = useParams<{ farmId: string }>();
  const navigate = useNavigate();
  const [data, setData] = useState<OpsFarmDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!getStoredOpsKey()) {
      navigate("/ops/login", { replace: true });
      return;
    }
    if (!farmId) return;
    opsApi.farmDetail(farmId).then(setData).catch((e) => {
      if (e instanceof OpsAuthError) navigate("/ops/login", { replace: true });
      else setError("Erro ao carregar dados da fazenda.");
    });
  }, [farmId, navigate]);

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center p-4">
        <p className="text-sm text-red-500">{error}</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background p-4 md:p-8 space-y-6 max-w-5xl mx-auto">
      <div className="flex items-center gap-3">
        <button
          onClick={() => navigate("/ops")}
          className="text-muted-foreground hover:text-foreground transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <div>
          <h1 className="font-heading text-xl font-bold">
            {data ? data.farmName : <Skeleton className="h-6 w-48 inline-block" />}
          </h1>
          <p className="text-xs text-muted-foreground">
            {data ? `Criada em ${formatDateTime(data.createdAt)}` : ""}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        {!data
          ? Array.from({ length: 4 }).map((_, i) => <Skeleton key={i} className="h-20 rounded-xl" />)
          : [
              { label: "Msgs 30d", value: data.messagesLast30d },
              { label: "Áudios 30d", value: data.audioLast30d },
              { label: "Transcritas 30d", value: data.transcribedLast30d },
              { label: "Erros 30d", value: data.failedLast30d, alert: data.failedLast30d > 0 },
            ].map(({ label, value, alert }) => (
              <div key={label} className={`rounded-xl p-4 ${alert ? "bg-red-50 border border-red-200" : "bg-muted/40"}`}>
                <p className={`text-xs font-medium ${alert ? "text-red-600" : "text-muted-foreground"}`}>{label}</p>
                <p className={`font-heading text-2xl font-bold mt-1 ${alert ? "text-red-600" : ""}`}>
                  {value.toLocaleString("pt-BR")}
                </p>
              </div>
            ))}
      </div>

      <Card>
        <CardHeader className="pb-2">
          <CardTitle className="text-sm font-heading font-semibold">Usuários</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b bg-muted/40">
                  <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Nome</th>
                  <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Role</th>
                  <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Ativo</th>
                  <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Última atividade WA</th>
                </tr>
              </thead>
              <tbody>
                {!data
                  ? Array.from({ length: 3 }).map((_, i) => (
                      <tr key={i} className="border-b">
                        {Array.from({ length: 4 }).map((_, j) => (
                          <td key={j} className="px-4 py-2.5"><Skeleton className="h-4 w-full" /></td>
                        ))}
                      </tr>
                    ))
                  : data.users.map((u) => (
                      <tr key={u.userId} className="border-b last:border-0">
                        <td className="px-4 py-2.5 font-medium">{u.name}</td>
                        <td className="px-4 py-2.5 text-muted-foreground capitalize">{u.role.toLowerCase()}</td>
                        <td className="px-4 py-2.5">
                          <span className={`text-xs px-1.5 py-0.5 rounded ${u.active ? "bg-emerald-100 text-emerald-700" : "bg-muted text-muted-foreground"}`}>
                            {u.active ? "Ativo" : "Inativo"}
                          </span>
                        </td>
                        <td className="px-4 py-2.5 text-muted-foreground">{formatDateTime(u.lastActivity)}</td>
                      </tr>
                    ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {data && data.recentErrors.length > 0 && (
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-heading font-semibold flex items-center gap-1.5">
              <AlertTriangle className="w-4 h-4 text-red-500" />
              Erros recentes
            </CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/40">
                    <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Recebida em</th>
                    <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Usuário</th>
                    <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Motivo</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentErrors.map((e) => (
                    <tr key={e.messageId} className="border-b last:border-0">
                      <td className="px-4 py-2.5 text-muted-foreground whitespace-nowrap">{formatDateTime(e.receivedAt)}</td>
                      <td className="px-4 py-2.5 font-mono text-xs">{e.userId ? `${e.userId.slice(0, 8)}…` : "—"}</td>
                      <td className="px-4 py-2.5 text-red-600 text-xs">{e.failureReason ?? "—"}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
