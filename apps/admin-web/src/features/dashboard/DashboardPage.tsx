import { useEffect, useState } from "react";
import {
  LineChart, Line, BarChart, Bar,
  XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend,
} from "recharts";
import { Beef, TrendingUp, Layers, Users as UsersIcon, TrendingDown, Minus, Activity } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import { useAuth } from "../auth/AuthContext";

type DashboardKpi = {
  totalAnimals: number;
  totalAnimalsChangePct?: number;
  activeAnimals: number;
  activeAnimalsChangePct?: number;
  lotsCount: number;
  lotsCountChangePct?: number;
  activeUsers: number;
  activeUsersChangePct?: number;
  avgDailyGain?: number | null;
  avgDailyGainChangePct?: number;
};

type WeightTrendPoint = {
  date: string;
  label: string;
  avgWeight: number;
  avgDailyGain: number | null;
};

type LotBreakdownEntry = {
  lotName: string;
  animalCount: number;
};

export function DashboardPage() {
  const { fetchProtectedJson } = useAuth();
  const [kpi, setKpi] = useState<DashboardKpi | null>(null);
  const [weightTrend, setWeightTrend] = useState<WeightTrendPoint[]>([]);
  const [lotBreakdown, setLotBreakdown] = useState<LotBreakdownEntry[]>([]);
  const [loadedAt, setLoadedAt] = useState<Date | null>(null);

  useEffect(() => {
    let active = true;

    async function load() {
      try {
        const data = await fetchProtectedJson<DashboardKpi>("/api/v1/dashboard");
        if (!active) return;
        setKpi(data);
        setLoadedAt(new Date());
      } catch {
        return;
      }

      // Chart fetches start only after KPI succeeds — keeps auth error flow clean
      void (async () => {
        try {
          const raw = await fetchProtectedJson<Array<{ date: string; avgWeight: number; avgDailyGain: number | null }>>("/api/v1/dashboard/weight-trend");
          if (active) setWeightTrend(raw.map((p) => ({ ...p, label: formatDateLabel(p.date) })));
        } catch {}
      })();

      void (async () => {
        try {
          const data = await fetchProtectedJson<LotBreakdownEntry[]>("/api/v1/dashboard/lot-breakdown");
          if (active) setLotBreakdown(data);
        } catch {}
      })();
    }

    void load();
    return () => { active = false; };
  }, [fetchProtectedJson]);

  const updatedLabel = loadedAt ? minutesAgo(loadedAt) : null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">Dashboard</h1>
        {updatedLabel && (
          <p className="text-xs text-muted-foreground mt-0.5">
            Visão geral da fazenda · {updatedLabel}
          </p>
        )}
      </div>

      {kpi == null ? (
        <div data-testid="kpi-skeleton" className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <Card key={i}>
              <CardContent className="p-4 space-y-2">
                <Skeleton className="h-3 w-24" />
                <Skeleton className="h-7 w-16" />
                <Skeleton className="h-3 w-20" />
              </CardContent>
            </Card>
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
          <KpiCard
            label="GMD Diário"
            value={kpi.avgDailyGain != null ? `${kpi.avgDailyGain.toFixed(2)} kg/dia` : "—"}
            changePct={kpi.avgDailyGainChangePct ?? 0}
            icon={Activity}
            highlight
          />
          <KpiCard label="Total de Animais" value={kpi.totalAnimals.toLocaleString("pt-BR")} changePct={kpi.totalAnimalsChangePct ?? 0} icon={Beef} />
          <KpiCard label="Animais Ativos" value={kpi.activeAnimals.toLocaleString("pt-BR")} changePct={kpi.activeAnimalsChangePct ?? 0} icon={TrendingUp} />
          <KpiCard label="Lotes" value={kpi.lotsCount.toLocaleString("pt-BR")} changePct={kpi.lotsCountChangePct ?? 0} icon={Layers} />
          <KpiCard label="Usuários Ativos" value={kpi.activeUsers.toLocaleString("pt-BR")} changePct={kpi.activeUsersChangePct ?? 0} icon={UsersIcon} />
        </div>
      )}

      <div className="grid lg:grid-cols-3 gap-5">
        <Card className="lg:col-span-2">
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-heading font-semibold">Tendência de Peso (30 dias)</CardTitle>
            <p className="text-xs text-muted-foreground">Peso médio do rebanho ao longo do tempo</p>
          </CardHeader>
          <CardContent>
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={weightTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                  <YAxis yAxisId="weight" tick={{ fontSize: 10 }} />
                  <YAxis yAxisId="gmd" orientation="right" tick={{ fontSize: 10 }} />
                  <Tooltip />
                  <Legend iconSize={8} wrapperStyle={{ fontSize: 11 }} formatter={(v) => v === "avgWeight" ? "Peso (kg)" : "GMD (kg/dia)"} />
                  <Line yAxisId="weight" type="monotone" dataKey="avgWeight" name="avgWeight" stroke="var(--primary)" strokeWidth={2} dot={false} />
                  <Line yAxisId="gmd" type="monotone" dataKey="avgDailyGain" name="avgDailyGain" stroke="oklch(0.75 0.17 160)" strokeWidth={2} dot={false} strokeDasharray="4 2" />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-heading font-semibold">Animais por Lote</CardTitle>
            <p className="text-xs text-muted-foreground">Distribuição atual do rebanho</p>
          </CardHeader>
          <CardContent>
            <div className="h-56">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={lotBreakdown} layout="vertical">
                  <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                  <XAxis type="number" tick={{ fontSize: 10 }} />
                  <YAxis type="category" dataKey="lotName" tick={{ fontSize: 10 }} width={72} />
                  <Tooltip />
                  <Bar dataKey="animalCount" fill="var(--primary)" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function KpiCard({
  label,
  value,
  changePct = 0,
  icon: Icon,
  highlight = false,
}: {
  label: string;
  value: string;
  changePct?: number;
  icon: React.ComponentType<{ className?: string }>;
  highlight?: boolean;
}) {
  const positive = changePct > 0;
  const negative = changePct < 0;
  const neutral = changePct === 0;

  return (
    <Card className={highlight ? "border-primary ring-1 ring-primary/30" : undefined}>
      <CardContent className="p-4">
        <div className="flex items-center justify-between mb-2">
          <span className="text-xs font-medium text-muted-foreground">{label}</span>
          <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${highlight ? "bg-primary/15" : "bg-muted"}`}>
            <Icon className={`w-4 h-4 ${highlight ? "text-primary" : "text-muted-foreground"}`} />
          </div>
        </div>
        <p className="font-heading text-xl font-bold">{value}</p>
        <div className={`flex items-center gap-1 mt-1.5 text-xs font-medium ${positive ? "text-emerald-600" : negative ? "text-red-500" : "text-muted-foreground"}`}>
          {positive && <TrendingUp className="w-3 h-3" />}
          {negative && <TrendingDown className="w-3 h-3" />}
          {neutral && <Minus className="w-3 h-3" />}
          <span>
            {neutral ? "0%" : `${positive ? "+" : ""}${changePct.toFixed(1)}%`} vs. mês anterior
          </span>
        </div>
      </CardContent>
    </Card>
  );
}

function formatDateLabel(dateStr: string): string {
  const [, , day] = dateStr.split("-");
  return `${day} Mai`;
}

function minutesAgo(date: Date): string {
  const mins = Math.floor((Date.now() - date.getTime()) / 60000);
  if (mins < 1) return "Atualizado agora";
  if (mins === 1) return "Atualizado há 1 minuto";
  return `Atualizado há ${mins} minutos`;
}
