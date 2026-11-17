import { useEffect, useState, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { LogOut, RefreshCw } from "lucide-react";
import { OpsSummarySection } from "./OpsSummarySection";
import { OpsFarmsTable } from "./OpsFarmsTable";
import { OpsMessageStatsSection } from "./OpsMessageStats";
import { OpsMessageErrors } from "./OpsMessageErrors";
import { opsApi, OpsAuthError, getStoredOpsKey, clearStoredOpsKey } from "./opsApi";
import type { OpsSummary, OpsFarmListItem } from "./opsApi";

function minutesAgo(date: Date): string {
  const mins = Math.floor((Date.now() - date.getTime()) / 60000);
  if (mins < 1) return "Atualizado agora";
  if (mins === 1) return "Atualizado há 1 minuto";
  return `Atualizado há ${mins} minutos`;
}

export function OpsPage() {
  const navigate = useNavigate();
  const [summary, setSummary] = useState<OpsSummary | null>(null);
  const [farms, setFarms] = useState<OpsFarmListItem[] | null>(null);
  const [loadedAt, setLoadedAt] = useState<Date | null>(null);
  const [updatedLabel, setUpdatedLabel] = useState<string | null>(null);

  const goToLogin = useCallback(() => {
    clearStoredOpsKey();
    navigate("/ops/login", { replace: true });
  }, [navigate]);

  const load = useCallback(async () => {
    if (!getStoredOpsKey()) {
      navigate("/ops/login", { replace: true });
      return;
    }
    setSummary(null);
    setFarms(null);
    try {
      const [s, f] = await Promise.all([opsApi.summary(), opsApi.farms()]);
      setSummary(s);
      setFarms(f);
      setLoadedAt(new Date());
    } catch (e) {
      if (e instanceof OpsAuthError) goToLogin();
    }
  }, [navigate, goToLogin]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    if (!loadedAt) return;
    setUpdatedLabel(minutesAgo(loadedAt));
    const interval = setInterval(() => setUpdatedLabel(minutesAgo(loadedAt)), 30_000);
    return () => clearInterval(interval);
  }, [loadedAt]);

  return (
    <div className="min-h-screen bg-background">
      <div className="border-b px-4 py-3 flex items-center justify-between">
        <div>
          <h1 className="font-heading text-lg font-bold">Plataforma — Ops</h1>
          {updatedLabel && (
            <p className="text-xs text-muted-foreground">{updatedLabel}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => void load()}
            className="p-2 rounded-lg text-muted-foreground hover:bg-muted transition-colors"
            title="Recarregar"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
          <button
            onClick={goToLogin}
            className="p-2 rounded-lg text-muted-foreground hover:bg-muted transition-colors"
            title="Sair"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>

      <div className="p-4 md:p-8 space-y-6 max-w-7xl mx-auto">
        <OpsSummarySection data={summary} />
        <OpsFarmsTable data={farms} />
        <OpsMessageStatsSection onAuthError={goToLogin} />
        <OpsMessageErrors onAuthError={goToLogin} />
      </div>
    </div>
  );
}
