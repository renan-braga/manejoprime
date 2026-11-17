import { useNavigate } from "react-router-dom";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import type { OpsFarmListItem } from "./opsApi";

function formatDate(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
}

function formatDateTime(iso: string | null): string {
  if (!iso) return "—";
  return new Date(iso).toLocaleString("pt-BR", {
    day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit"
  });
}

export function OpsFarmsTable({ data }: { data: OpsFarmListItem[] | null }) {
  const navigate = useNavigate();

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-sm font-heading font-semibold">Fazendas</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-muted/40">
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Fazenda</th>
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Criada em</th>
                <th className="px-4 py-2.5 text-right text-xs font-medium text-muted-foreground">Usuários</th>
                <th className="px-4 py-2.5 text-right text-xs font-medium text-muted-foreground">Animais</th>
                <th className="px-4 py-2.5 text-left text-xs font-medium text-muted-foreground">Última atividade WA</th>
                <th className="px-4 py-2.5 text-right text-xs font-medium text-muted-foreground">Msgs 7d</th>
                <th className="px-4 py-2.5 text-right text-xs font-medium text-muted-foreground">Erros 7d</th>
              </tr>
            </thead>
            <tbody>
              {data == null
                ? Array.from({ length: 5 }).map((_, i) => (
                    <tr key={i} className="border-b">
                      {Array.from({ length: 7 }).map((_, j) => (
                        <td key={j} className="px-4 py-2.5">
                          <Skeleton className="h-4 w-full" />
                        </td>
                      ))}
                    </tr>
                  ))
                : data.map((farm) => (
                    <tr
                      key={farm.farmId}
                      className="border-b last:border-0 hover:bg-muted/30 cursor-pointer transition-colors"
                      onClick={() => navigate(`/ops/farms/${farm.farmId}`)}
                    >
                      <td className="px-4 py-2.5 font-medium">{farm.farmName}</td>
                      <td className="px-4 py-2.5 text-muted-foreground">{formatDate(farm.createdAt)}</td>
                      <td className="px-4 py-2.5 text-right">{farm.userCount}</td>
                      <td className="px-4 py-2.5 text-right">{farm.animalCount}</td>
                      <td className="px-4 py-2.5 text-muted-foreground">{formatDateTime(farm.lastWhatsappActivity)}</td>
                      <td className="px-4 py-2.5 text-right">{farm.messagesLast7d}</td>
                      <td className={`px-4 py-2.5 text-right font-medium ${farm.errorsLast7d > 0 ? "text-red-500" : "text-muted-foreground"}`}>
                        {farm.errorsLast7d}
                      </td>
                    </tr>
                  ))}
            </tbody>
          </table>
        </div>
      </CardContent>
    </Card>
  );
}
