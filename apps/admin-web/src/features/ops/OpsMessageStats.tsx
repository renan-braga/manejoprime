import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import { opsApi, OpsAuthError } from "./opsApi";
import type { OpsMessageStats } from "./opsApi";

type Period = "24h" | "7d" | "30d";

const PERIODS: { value: Period; label: string }[] = [
  { value: "24h", label: "24 horas" },
  { value: "7d", label: "7 dias" },
  { value: "30d", label: "30 dias" },
];

export function OpsMessageStatsSection({ onAuthError }: { onAuthError: () => void }) {
  const [period, setPeriod] = useState<Period>("7d");
  const [data, setData] = useState<OpsMessageStats | null>(null);

  useEffect(() => {
    setData(null);
    opsApi.messageStats(period).then(setData).catch((e) => {
      if (e instanceof OpsAuthError) onAuthError();
    });
  }, [period, onAuthError]);

  const transcriptionRate =
    data && data.audioMessages > 0
      ? ((data.transcribed / data.audioMessages) * 100).toFixed(1)
      : null;

  return (
    <Card>
      <CardHeader className="pb-2 flex flex-row items-center justify-between">
        <CardTitle className="text-sm font-heading font-semibold">Pipeline WhatsApp</CardTitle>
        <div className="flex gap-1">
          {PERIODS.map((p) => (
            <button
              key={p.value}
              onClick={() => setPeriod(p.value)}
              className={`px-2.5 py-1 text-xs rounded-md font-medium transition-colors ${
                period === p.value
                  ? "bg-primary text-primary-foreground"
                  : "bg-muted text-muted-foreground hover:bg-muted/80"
              }`}
            >
              {p.label}
            </button>
          ))}
        </div>
      </CardHeader>
      <CardContent>
        {!data ? (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {Array.from({ length: 7 }).map((_, i) => (
              <Skeleton key={i} className="h-16 w-full" />
            ))}
          </div>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <StatBox label="Total recebidas" value={data.totalMessages} />
            <StatBox label="Áudios" value={data.audioMessages} />
            <StatBox label="Texto" value={data.textMessages} />
            <StatBox label="Transcritas" value={data.transcribed}
              sub={transcriptionRate ? `${transcriptionRate}% dos áudios` : undefined} />
            <StatBox label="Aplicadas" value={data.applied} positive />
            <StatBox label="Falharam" value={data.failed} alert={data.failed > 0} />
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function StatBox({
  label, value, sub, positive, alert
}: {
  label: string;
  value: number;
  sub?: string;
  positive?: boolean;
  alert?: boolean;
}) {
  return (
    <div className={`rounded-lg p-3 ${alert ? "bg-red-50 border border-red-200" : "bg-muted/40"}`}>
      <p className={`text-xs font-medium ${alert ? "text-red-600" : "text-muted-foreground"}`}>{label}</p>
      <p className={`font-heading text-xl font-bold mt-1 ${positive ? "text-emerald-600" : alert ? "text-red-600" : ""}`}>
        {value.toLocaleString("pt-BR")}
      </p>
      {sub && <p className="text-[10px] text-muted-foreground mt-0.5">{sub}</p>}
    </div>
  );
}
