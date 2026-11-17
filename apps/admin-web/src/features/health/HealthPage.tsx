import { AlertTriangle, Pill, Syringe } from "lucide-react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { usePageTour } from "@/hooks/usePageTour";
import { AppTour } from "../shell/AppTour";
import type { Step } from "react-joyride";

const HEALTH_TOUR_STEPS: Step[] = [
  {
    target: "[data-tour='health-cards']",
    title: "Módulos de saúde",
    content: "Escolha um módulo: registre ocorrências de saúde, aplique vacinas ou gerencie medicamentos do rebanho.",
    placement: "bottom",
  },
];

type HealthWorkflow = {
  href: string;
  label: string;
  permission: string;
  summary: string;
  icon: React.ComponentType<{ className?: string }>;
};

const HEALTH_WORKFLOWS: HealthWorkflow[] = [
  {
    href: "/app/health/occurrences",
    label: "Ocorrências",
    permission: "occurrences.write",
    summary: "Revise eventos de saúde e resolva ou reabra casos por papel.",
    icon: AlertTriangle,
  },
  {
    href: "/app/health/vaccines",
    label: "Vacinas",
    permission: "vaccines.write",
    summary: "Registre doses para animais ou lotes e acompanhe os reforços.",
    icon: Syringe,
  },
  {
    href: "/app/health/medications",
    label: "Medicamentos",
    permission: "medications.write",
    summary: "Acompanhe tratamentos, ocorrências vinculadas e períodos de carência.",
    icon: Pill,
  },
];

export function HealthPage() {
  const { session } = useAuth();
  const { run: runTour, finish: finishTour } = usePageTour("health");
  const permissions = session?.permissions ?? [];
  const canOpenHealthPage =
    permissions.includes("occurrences.write") ||
    permissions.includes("vaccines.write") ||
    permissions.includes("medications.write") ||
    permissions.includes("reports.health.read");
  const visibleWorkflows = HEALTH_WORKFLOWS.filter((workflow) => permissions.includes(workflow.permission));

  if (!canOpenHealthPage) return null;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-heading text-2xl font-bold tracking-tight">Saúde</h1>
        <p className="text-sm text-muted-foreground">
          Os fluxos de saúde estão em rotas dedicadas.
        </p>
      </div>

      {visibleWorkflows.length === 0 ? (
        <div className="rounded-lg border border-border bg-card p-6">
          <h2 className="font-heading text-lg font-semibold">Operações de saúde são controladas por papel.</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            Esta sessão pode acessar a navegação de saúde, mas os fluxos operacionais estão reservados para os papéis que os gerenciam.
          </p>
        </div>
      ) : (
        <div className="grid gap-3 md:grid-cols-3" data-tour="health-cards">
          {visibleWorkflows.map((workflow) => (
            <Link
              className="rounded-lg border border-border bg-card p-4 transition-colors hover:bg-muted/40"
              key={workflow.href}
              to={workflow.href}
            >
              <workflow.icon className="mb-3 h-6 w-6 text-primary" />
              <h2 className="font-heading text-lg font-semibold">{workflow.label}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{workflow.summary}</p>
            </Link>
          ))}
        </div>
      )}
      <AppTour steps={HEALTH_TOUR_STEPS} run={runTour} onFinish={finishTour} />
    </div>
  );
}
