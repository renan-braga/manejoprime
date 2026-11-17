import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Button } from "../../components/ui/button";
import { Input } from "../../components/ui/input";
import { Label } from "../../components/ui/label";
import { opsApi, setStoredOpsKey } from "./opsApi";

export function OpsLoginPage() {
  const [key, setKey] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!key.trim()) return;
    setLoading(true);
    setError(null);
    setStoredOpsKey(key.trim());
    try {
      await opsApi.summary();
      navigate("/ops");
    } catch {
      setError("Chave inválida. Verifique o valor de OPS_API_KEY e tente novamente.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4">
      <Card className="w-full max-w-sm">
        <CardHeader>
          <CardTitle className="font-heading text-lg">Acesso Ops</CardTitle>
          <p className="text-xs text-muted-foreground mt-0.5">
            Painel de contingência — uso restrito ao operador da plataforma.
          </p>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="ops-key">Chave de acesso</Label>
              <Input
                id="ops-key"
                type="password"
                placeholder="OPS_API_KEY"
                value={key}
                onChange={(e) => setKey(e.target.value)}
                autoComplete="off"
                autoFocus
              />
            </div>
            {error && (
              <p className="text-xs text-red-500">{error}</p>
            )}
            <Button type="submit" className="w-full" disabled={loading || !key.trim()}>
              {loading ? "Verificando..." : "Entrar"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
