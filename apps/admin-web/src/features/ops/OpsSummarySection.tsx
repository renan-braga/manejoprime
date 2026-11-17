import {
  Building2, Users, Beef, MessageSquare, Mic, AlertTriangle, FileAudio
} from "lucide-react";
import { Card, CardContent } from "../../components/ui/card";
import { Skeleton } from "../../components/ui/skeleton";
import type { OpsSummary } from "./opsApi";

export function OpsSummarySection({ data }: { data: OpsSummary | null }) {
  if (!data) {
    return (
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-3">
        {Array.from({ length: 8 }).map((_, i) => (
          <Card key={i}>
            <CardContent className="p-4 space-y-2">
              <Skeleton className="h-3 w-24" />
              <Skeleton className="h-7 w-16" />
            </CardContent>
          </Card>
        ))}
      </div>
    );
  }

  const cards = [
    { label: "Fazendas", value: data.totalFarms, icon: Building2 },
    { label: "Usuários ativos", value: data.totalUsers, icon: Users,
      sub: `${data.adminCount} admin · ${data.vetCount} vet · ${data.workerCount} func.` },
    { label: "Animais ativos", value: data.totalAnimals, icon: Beef },
    { label: "Msgs 24h", value: data.messagesLast24h, icon: MessageSquare },
    { label: "Msgs 7 dias", value: data.messagesLast7d, icon: MessageSquare },
    { label: "Msgs 30 dias", value: data.messagesLast30d, icon: MessageSquare },
    { label: "Áudios 30d", value: data.audioCount, icon: FileAudio },
    { label: "Transcrições 30d", value: data.transcriptionCount, icon: Mic },
    { label: "Erros 30d", value: data.errorCount, icon: AlertTriangle, alert: data.errorCount > 0 },
  ];

  return (
    <div className="grid grid-cols-2 lg:grid-cols-5 gap-3">
      {cards.map(({ label, value, icon: Icon, sub, alert }) => (
        <Card key={label} className={alert ? "border-red-400 ring-1 ring-red-400/30" : undefined}>
          <CardContent className="p-4">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-medium text-muted-foreground">{label}</span>
              <div className={`w-8 h-8 rounded-lg flex items-center justify-center ${alert ? "bg-red-100" : "bg-muted"}`}>
                <Icon className={`w-4 h-4 ${alert ? "text-red-500" : "text-muted-foreground"}`} />
              </div>
            </div>
            <p className="font-heading text-xl font-bold">{value.toLocaleString("pt-BR")}</p>
            {sub && <p className="text-[10px] text-muted-foreground mt-1">{sub}</p>}
          </CardContent>
        </Card>
      ))}
    </div>
  );
}
