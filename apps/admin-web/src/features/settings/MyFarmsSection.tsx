import { useEffect, useState } from "react";
import { useAuth } from "../auth/AuthContext";
import { describeApiError } from "@/lib/apiErrors";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Skeleton } from "@/components/ui/skeleton";
import { Barn } from "@phosphor-icons/react";

type FarmEntry = {
  farmId: string;
  farmName: string;
  role: string;
  isCurrent: boolean;
};

type MyFarmsResponse = {
  farms: FarmEntry[];
  totalFarms: number;
  maxFarms: number;
};

export function MyFarmsSection() {
  const { fetchProtectedJson, switchFarm, session } = useAuth();

  const [data, setData] = useState<MyFarmsResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const [showForm, setShowForm] = useState(false);
  const [newFarmName, setNewFarmName] = useState("");
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);

  const [switching, setSwitching] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      try {
        const result = await fetchProtectedJson<MyFarmsResponse>("/api/v1/my-farms");
        if (!cancelled) {
          setData(result);
          setLoadError(null);
        }
      } catch (error) {
        if (!cancelled) setLoadError(describeApiError(error));
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    void load();
    return () => { cancelled = true; };
  }, [fetchProtectedJson]);

  const canAddFarm = data != null && (data.maxFarms < 0 || data.totalFarms < data.maxFarms);

  async function handleAddFarm() {
    if (!newFarmName.trim()) return;
    setAdding(true);
    setAddError(null);
    try {
      const created = await fetchProtectedJson<FarmEntry>("/api/v1/my-farms", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ farmName: newFarmName.trim() })
      });
      setData((prev) => prev == null ? null : {
        ...prev,
        farms: [...prev.farms, created],
        totalFarms: prev.totalFarms + 1
      });
      setNewFarmName("");
      setShowForm(false);
      window.dispatchEvent(new Event("farms-updated"));
    } catch (error) {
      setAddError(describeApiError(error));
    } finally {
      setAdding(false);
    }
  }

  async function handleSwitch(farmId: string) {
    setSwitching(farmId);
    try {
      await switchFarm(farmId);
      window.location.reload();
    } catch (error) {
      setAddError(describeApiError(error));
    } finally {
      setSwitching(null);
    }
  }

  return (
    <div id="fazendas" className="overflow-hidden rounded-xl border border-border bg-card">
      <div className="flex items-center justify-between px-6 py-4 border-b border-border">
        <div>
          <h2 className="font-heading text-base font-semibold">Minhas Fazendas</h2>
          <p className="text-sm text-muted-foreground">
            {data != null && data.maxFarms > 0
              ? `${data.totalFarms} de ${data.maxFarms} fazendas utilizadas`
              : "Gerencie suas fazendas"}
          </p>
        </div>
        {canAddFarm && session?.role === "admin" && (
          <Button
            onClick={() => setShowForm((prev) => !prev)}
            size="sm"
            type="button"
            variant="outline"
          >
            Adicionar fazenda
          </Button>
        )}
      </div>

      {loading ? (
        <div className="p-6 space-y-3">
          <Skeleton className="h-10 w-full" />
          <Skeleton className="h-10 w-full" />
        </div>
      ) : loadError != null ? (
        <p className="px-6 py-4 text-sm text-destructive">{loadError}</p>
      ) : (
        <ul className="divide-y divide-border">
          {data?.farms.map((farm) => (
            <li key={farm.farmId} className="flex items-center justify-between px-6 py-3">
              <div className="flex items-center gap-3">
                <Barn size={20} weight="duotone" className="text-muted-foreground shrink-0" />
                <div>
                  <p className="text-sm font-medium">{farm.farmName}</p>
                  <p className="text-xs text-muted-foreground capitalize">{farm.role}</p>
                </div>
                {farm.isCurrent && (
                  <span className="text-xs rounded-full bg-primary/10 text-primary px-2 py-0.5 font-medium">
                    Atual
                  </span>
                )}
              </div>
              {!farm.isCurrent && (
                <Button
                  disabled={switching === farm.farmId}
                  onClick={() => { void handleSwitch(farm.farmId); }}
                  size="sm"
                  type="button"
                  variant="outline"
                >
                  {switching === farm.farmId ? "Trocando..." : "Trocar"}
                </Button>
              )}
            </li>
          ))}
        </ul>
      )}

      {showForm && (
        <div className="border-t border-border px-6 py-4 space-y-3">
          <div className="space-y-1.5">
            <Label htmlFor="new-farm-name">Nome da nova fazenda</Label>
            <Input
              id="new-farm-name"
              maxLength={255}
              onChange={(e) => setNewFarmName(e.target.value)}
              placeholder="Ex: Fazenda Santa Cruz"
              value={newFarmName}
            />
          </div>
          {addError != null && <p className="text-sm text-destructive">{addError}</p>}
          <div className="flex gap-2">
            <Button
              disabled={adding || !newFarmName.trim()}
              onClick={() => { void handleAddFarm(); }}
              size="sm"
              type="button"
            >
              {adding ? "Salvando..." : "Salvar"}
            </Button>
            <Button
              onClick={() => { setShowForm(false); setNewFarmName(""); setAddError(null); }}
              size="sm"
              type="button"
              variant="ghost"
            >
              Cancelar
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
