import { useEffect, useMemo, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError } from "@/lib/apiErrors";
import { capitalize, formatOccurrenceStatus, formatOccurrenceSource } from "@/lib/formatters";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

type DashboardResponse = {
  totalAnimals: number;
  activeAnimals: number;
  lotsCount: number;
  activeUsers: number;
  statusBreakdown: {
    active: number;
    sold: number;
    dead: number;
    transferred: number;
  };
  alerts: Array<{
    type: string;
    title: string;
    count: number;
  }>;
  shortcuts: string[];
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

const SEVERITY_VARIANT: Record<string, "destructive" | "secondary" | "outline"> = {
  critical: "destructive",
  medium: "secondary",
  low: "outline"
};

const STATUS_VARIANT: Record<string, "default" | "secondary" | "outline"> = {
  open: "default",
  in_follow_up: "secondary",
  resolved: "outline"
};

export function ReportsPage() {
  const { fetchProtectedJson, session } = useAuth();
  const permissions = session?.permissions ?? [];
  const canViewAdminReports = permissions.includes("reports.read");
  const canViewHealthReports = permissions.includes("reports.health.read") || permissions.includes("occurrences.write");
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [occurrences, setOccurrences] = useState<OccurrenceListResponse | null>(null);
  const [dashboardError, setDashboardError] = useState<string | null>(null);
  const [occurrencesError, setOccurrencesError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadDashboard() {
      try {
        const nextDashboard = await fetchProtectedJson<DashboardResponse>("/api/v1/dashboard");
        if (!cancelled) {
          setDashboard(nextDashboard);
          setDashboardError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setDashboard(null);
          setDashboardError(describeApiError(error, "Não foi possível carregar o resumo do relatório."));
        }
      }
    }

    void loadDashboard();

    return () => {
      cancelled = true;
    };
  }, [fetchProtectedJson]);

  useEffect(() => {
    if (!canViewHealthReports) {
      setOccurrences(null);
      setOccurrencesError(null);
      return;
    }

    let cancelled = false;

    async function loadOccurrences() {
      try {
        const nextOccurrences = await fetchProtectedJson<OccurrenceListResponse>("/api/v1/occurrences?limit=20&offset=0");
        if (!cancelled) {
          setOccurrences(nextOccurrences);
          setOccurrencesError(null);
        }
      } catch (error) {
        if (!cancelled) {
          setOccurrences(null);
          setOccurrencesError(describeApiError(error, "Não foi possível carregar o relatório de acompanhamento de saúde."));
        }
      }
    }

    void loadOccurrences();

    return () => {
      cancelled = true;
    };
  }, [canViewHealthReports, fetchProtectedJson]);

  const occurrenceSummary = useMemo(() => {
    const items = occurrences?.items ?? [];
    return {
      critical: items.filter((o) => o.severity === "critical").length,
      open: items.filter((o) => o.status === "open").length,
      followUp: items.filter((o) => o.status === "in_follow_up").length,
      resolved: items.filter((o) => o.status === "resolved").length
    };
  }, [occurrences]);

  const loadingDashboard = dashboard == null && dashboardError == null;

  return (
    <div className="space-y-6">
        <h1 className="font-heading text-2xl font-bold tracking-tight">Relatórios</h1>

        {loadingDashboard ? (
          <div data-testid="reports-skeleton" className="space-y-4">
            <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
              {Array.from({ length: 4 }).map((_, i) => (
                <Skeleton key={i} className="h-24 rounded-xl" />
              ))}
            </div>
            <div className="grid gap-6 md:grid-cols-2">
              <Skeleton className="h-48 rounded-xl" />
              <Skeleton className="h-48 rounded-xl" />
            </div>
          </div>
        ) : (
          <div className="space-y-6">
            {canViewAdminReports ? (
              <>
                <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                  <MetricCard label="Animais" value={dashboard?.totalAnimals ?? 0} />
                  <MetricCard label="Rebanho ativo" value={dashboard?.activeAnimals ?? 0} />
                  <MetricCard label="Lotes" value={dashboard?.lotsCount ?? 0} />
                  <MetricCard label="Usuários com acesso" value={dashboard?.activeUsers ?? 0} />
                </div>

                <div className="grid gap-6 md:grid-cols-2">
                  <Card>
                    <CardHeader>
                      <div className="flex items-center justify-between">
                        <CardTitle>Resumo da fazenda</CardTitle>
                        <span className="text-sm text-muted-foreground">{session?.farmName ?? "Contexto da fazenda indisponível"}</span>
                      </div>
                      <p className="text-sm text-muted-foreground">
                        Os totais do painel agrupados para facilitar a visualização.
                      </p>
                    </CardHeader>
                    <CardContent>
                      {dashboardError != null ? (
                        <p className="text-sm text-destructive">{dashboardError}</p>
                      ) : dashboard != null ? (
                        <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                          <StatusStat label="Ativos" value={dashboard.statusBreakdown.active} />
                          <StatusStat label="Vendidos" value={dashboard.statusBreakdown.sold} />
                          <StatusStat label="Mortos" value={dashboard.statusBreakdown.dead} />
                          <StatusStat label="Transferidos" value={dashboard.statusBreakdown.transferred} />
                        </div>
                      ) : null}
                    </CardContent>
                  </Card>

                  <Card>
                    <CardHeader>
                      <CardTitle>Monitoramento operacional</CardTitle>
                      <p className="text-sm text-muted-foreground">
                        Alertas do rebanho disponíveis aqui enquanto o módulo de exportação é preparado.
                      </p>
                    </CardHeader>
                    <CardContent>
                      {dashboardError != null ? (
                        <p className="text-sm text-destructive">{dashboardError}</p>
                      ) : dashboard != null ? (
                        dashboard.alerts.length === 0 ? (
                          <p className="text-sm text-muted-foreground">
                            Nenhum alerta disponível para esta fazenda no momento.
                          </p>
                        ) : (
                          <div className="space-y-2">
                            {dashboard.alerts.map((alert) => (
                              <div className="flex items-center justify-between" key={`${alert.type}-${alert.title}`}>
                                <strong className="text-sm">{alert.title}</strong>
                                <span className="text-sm font-medium">{alert.count}</span>
                              </div>
                            ))}
                          </div>
                        )
                      ) : null}
                    </CardContent>
                  </Card>
                </div>
              </>
            ) : null}

            {canViewHealthReports ? (
              <div className="grid gap-6 md:grid-cols-2">
                <Card>
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle>Acompanhamento de saúde</CardTitle>
                      <span className="text-sm text-muted-foreground">Fila de saúde</span>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      Casos críticos e ocorrências em aberto para visualização rápida.
                    </p>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="grid grid-cols-2 gap-4 md:grid-cols-4">
                      <MetricCard label="Acompanhamento crítico" value={occurrenceSummary.critical} />
                      <MetricCard label="Em aberto" value={occurrenceSummary.open} />
                      <MetricCard label="Em acompanhamento" value={occurrenceSummary.followUp} />
                      <MetricCard label="Resolvidos visíveis" value={occurrenceSummary.resolved} />
                    </div>

                    {occurrencesError != null ? (
                      <p className="text-sm text-destructive">{occurrencesError}</p>
                    ) : occurrences != null && occurrences.items.length === 0 ? (
                      <p className="text-sm text-muted-foreground">
                        Nenhum item de acompanhamento de saúde encontrado.
                      </p>
                    ) : null}

                    <div className="space-y-3">
                      {occurrences?.items.map((occurrence) => (
                        <div className="rounded-lg border p-3 space-y-1" key={occurrence.id}>
                          <div className="flex items-start justify-between gap-2">
                            <div>
                              <p className="text-sm font-semibold">{formatOccurrenceType(occurrence.type)}</p>
                              <p className="text-xs text-muted-foreground">{`Animal ${occurrence.animalIdentifier} · ${occurrence.animalName ?? "Sem nome"}`}</p>
                            </div>
                            <div className="flex gap-1 shrink-0">
                              <Badge variant={SEVERITY_VARIANT[occurrence.severity] ?? "outline"}>
                                {capitalize(occurrence.severity)}
                              </Badge>
                              <Badge variant={STATUS_VARIANT[occurrence.status] ?? "outline"}>
                                {formatOccurrenceStatus(occurrence.status)}
                              </Badge>
                            </div>
                          </div>
                          <p className="text-xs">{occurrence.description}</p>
                          <p className="text-xs text-muted-foreground">
                            {formatOccurrenceSource(occurrence.source)} · Atualizado em {formatTimestamp(occurrence.updatedAt)}
                          </p>
                        </div>
                      ))}
                    </div>
                  </CardContent>
                </Card>

                <Card>
                  <CardHeader>
                    <CardTitle>Exportações</CardTitle>
                    <p className="text-sm text-muted-foreground">
                      Exportações de relatórios serão disponibilizadas em breve.
                    </p>
                  </CardHeader>
                  <CardContent className="space-y-4">
                    <div className="rounded-lg border p-4 space-y-2">
                      <p className="text-sm font-semibold">Exportação de saúde</p>
                      <p className="text-xs text-muted-foreground">
                        Acompanhamento de ocorrências, mix de severidade e cadência de recuperação estarão disponíveis quando as exportações forem implementadas.
                      </p>
                      <Button variant="outline" size="sm" disabled>
                        Em breve
                      </Button>
                    </div>

                    {canViewAdminReports ? (
                      <div className="rounded-lg border p-4 space-y-2">
                        <p className="text-sm font-semibold">Exportação de operações</p>
                        <p className="text-xs text-muted-foreground">
                          Totais da fazenda, acesso de usuários e mix operacional estarão disponíveis em uma versão futura.
                        </p>
                        <Button variant="outline" size="sm" disabled>
                          Em breve
                        </Button>
                      </div>
                    ) : null}
                  </CardContent>
                </Card>
              </div>
            ) : null}
          </div>
        )}
    </div>
  );
}

function MetricCard({ label, value }: { label: string; value: number }) {
  return (
    <Card>
      <CardContent className="pt-4">
        <p className="text-xs text-muted-foreground">{label}</p>
        <p className="text-2xl font-bold">{value}</p>
      </CardContent>
    </Card>
  );
}

function StatusStat({ label, value }: { label: string; value: number }) {
  return (
    <div className="space-y-1">
      <p className="text-xs text-muted-foreground">{label}</p>
      <p className="text-lg font-semibold">{value}</p>
    </div>
  );
}

function formatOccurrenceType(value: string) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function formatTimestamp(value: string) {
  return value.replace("T", " ").replace(/:00(?:\.\d+)?(?:Z|[+-]\d\d:\d\d)?$/, "");
}

