import { useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { NavLink, Link, Outlet, useNavigate } from "react-router-dom";
import { OnboardingWizardModal } from "../onboarding/OnboardingWizardModal";
import { useOnboardingStatus } from "../onboarding/useOnboardingStatus";
import { AppTour } from "./AppTour";
import type { Step } from "react-joyride";
import { motion } from "framer-motion";
import {
  LayoutDashboard,
  Beef,
  Layers,
  HeartPulse,
  AlertTriangle,
  Syringe,
  Pill,
  HandCoins,
  BarChart3,
  Scale,
  Wheat,
  Users as UsersIcon,
  Settings as SettingsIcon,
  PanelLeftClose,
  PanelLeftOpen,
  Sun,
  Moon,
  LogOut,
  Shield,
  CreditCard,
  X,
  ChevronDown,
  Check,
  Plus,
} from "lucide-react";
import { useAuth } from "../auth/AuthContext";
import { useTheme } from "../theme/ThemeContext";
import { hasAnyPermission } from "../auth/permissions";
import { Toaster } from "@/components/ui/sonner";
import { BrandLogo } from "@/components/BrandLogo";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { FloatingWhatsAppBubbles } from "@/components/FloatingWhatsAppBubbles";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { describeApiError } from "@/lib/apiErrors";

type NavItem = {
  label: string;
  path: string;
  icon: React.ComponentType<{ className?: string }>;
  requiredPermissions: string[];
};

type NavGroup = {
  label: string;
  tourId: string;
  items: NavItem[];
};

const SIDEBAR_TOUR_STEPS: Step[] = [
  {
    target: "[data-tour='nav-fazenda']",
    title: "Fazenda",
    content: "Gerencie animais, lotes, pesagens e alimentação do seu rebanho.",
    placement: "right",
  },
  {
    target: "[data-tour='nav-saude']",
    title: "Saúde",
    content: "Registre ocorrências, vacinas e medicamentos do rebanho.",
    placement: "right",
  },
  {
    target: "[data-tour='nav-comercio']",
    title: "Ofertas",
    content: "Publique ofertas de venda de animais e acesse relatórios produtivos.",
    placement: "right",
  },
  {
    target: "[data-tour='nav-administracao']",
    title: "Administração",
    content: "Gerencie usuários, plano de assinatura e configurações da fazenda.",
    placement: "right",
  },
];

const NAV_GROUPS: NavGroup[] = [
  {
    label: "Fazenda",
    tourId: "nav-fazenda",
    items: [
      { label: "Painel", path: "/app", icon: LayoutDashboard, requiredPermissions: ["dashboard.read"] },
      { label: "Animais", path: "/app/animals", icon: Beef, requiredPermissions: ["animals.read"] },
      { label: "Lotes", path: "/app/lots", icon: Layers, requiredPermissions: ["lots.read"] },
      { label: "Pesagens", path: "/app/animals/weighings", icon: Scale, requiredPermissions: ["weighings.write"] },
      { label: "Alimentação", path: "/app/animals/feedings", icon: Wheat, requiredPermissions: ["feeding.write"] },
    ],
  },
  {
    label: "Saúde",
    tourId: "nav-saude",
    items: [
      { label: "Saúde", path: "/app/health", icon: HeartPulse, requiredPermissions: ["occurrences.write", "vaccines.write", "medications.write", "reports.health.read"] },
      { label: "Ocorrências", path: "/app/health/occurrences", icon: AlertTriangle, requiredPermissions: ["occurrences.write"] },
      { label: "Vacinas", path: "/app/health/vaccines", icon: Syringe, requiredPermissions: ["vaccines.write"] },
      { label: "Medicamentos", path: "/app/health/medications", icon: Pill, requiredPermissions: ["medications.write"] },
    ],
  },
  {
    label: "Comércio",
    tourId: "nav-comercio",
    items: [
      { label: "Ofertas", path: "/app/offers", icon: HandCoins, requiredPermissions: ["offers.read"] },
      { label: "Relatórios", path: "/app/reports", icon: BarChart3, requiredPermissions: ["reports.read", "reports.health.read"] },
    ],
  },
  {
    label: "Administração",
    tourId: "nav-administracao",
    items: [
      { label: "Usuários", path: "/app/users", icon: UsersIcon, requiredPermissions: ["users.read"] },
      { label: "Plano", path: "/app/billing", icon: CreditCard, requiredPermissions: ["settings.read"] },
      { label: "Configurações", path: "/app/settings", icon: SettingsIcon, requiredPermissions: ["settings.read"] },
    ],
  },
];

const ROLE_LABELS: Record<string, string> = {
  admin: "Admin",
  veterinarian: "Veterinário",
  worker: "Funcionário",
};

const ROLE_COLORS: Record<string, string> = {
  admin: "bg-admin-light text-admin",
  veterinarian: "bg-vet-light text-vet",
  worker: "bg-func-light text-func",
};

function trialDaysRemaining(periodEnd: string) {
  return Math.max(0, Math.ceil((new Date(periodEnd).getTime() - Date.now()) / (1000 * 60 * 60 * 24)));
}

function TrialChip({ periodEnd }: { periodEnd: string }) {
  const days = trialDaysRemaining(periodEnd);
  return (
    <Link
      to="/app/billing"
      className="inline-flex items-center gap-1.5 rounded-full border border-amber-300 bg-amber-50 px-2.5 py-1 text-xs font-semibold text-amber-700 hover:bg-amber-100 transition-colors dark:border-amber-700 dark:bg-amber-950/30 dark:text-amber-300"
    >
      <span className="hidden sm:inline">Teste grátis · {days} {days === 1 ? "dia" : "dias"}</span>
      <span className="sm:hidden">{days}d</span>
    </Link>
  );
}

function TrialBanner({ periodEnd }: { periodEnd: string }) {
  const days = trialDaysRemaining(periodEnd);
  const urgent = days <= 5;
  return (
    <div className={`flex items-center justify-between gap-3 border-b px-4 py-2 text-sm ${
      urgent
        ? "border-red-200 bg-red-50 text-red-900 dark:border-red-800 dark:bg-red-950/30 dark:text-red-200"
        : "border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-800 dark:bg-amber-950/30 dark:text-amber-200"
    }`}>
      <span className="font-medium">
        {urgent
          ? `⚠ Seu teste encerra em ${days} ${days === 1 ? "dia" : "dias"} — assine para não perder o acesso.`
          : `Teste grátis · ${days} ${days === 1 ? "dia restante" : "dias restantes"}.`}
      </span>
      <Link
        to="/app/billing"
        className={`shrink-0 rounded-md px-3 py-1 text-xs font-semibold transition-colors ${
          urgent
            ? "bg-red-600 text-white hover:bg-red-700"
            : "bg-amber-600 text-white hover:bg-amber-700"
        }`}
      >
        Assinar agora
      </Link>
    </div>
  );
}

type BillingStatusConfig = {
  dot: string;
  label: string;
  blocked: boolean;
  strip: string;
  iconBg: string;
  iconColor: string;
  labelColor: string;
  title: string;
  description: string;
};

function billingStatusConfig(status: string | undefined, canWrite: boolean): BillingStatusConfig | null {
  switch (status) {
    case "active":
      return {
        dot: "bg-emerald-500",
        label: "Ativo",
        blocked: false,
        strip: "bg-emerald-500",
        iconBg: "bg-emerald-100 dark:bg-emerald-900/40",
        iconColor: "text-emerald-600 dark:text-emerald-400",
        labelColor: "text-emerald-600 dark:text-emerald-400",
        title: "Plano em dia",
        description: "Sua assinatura está ativa. Todos os recursos estão disponíveis.",
      };
    case "past_due":
      return {
        dot: "bg-amber-500",
        label: "Atenção",
        blocked: false,
        strip: "bg-amber-500",
        iconBg: "bg-amber-100 dark:bg-amber-900/40",
        iconColor: "text-amber-600 dark:text-amber-400",
        labelColor: "text-amber-600 dark:text-amber-400",
        title: "Pagamento pendente",
        description: "Há um pagamento em aberto. Regularize para evitar a suspensão da conta.",
      };
    case "suspended":
    case "payment_required":
    case "canceled":
      return {
        dot: "bg-destructive",
        label: "Suspenso",
        blocked: !canWrite,
        strip: "bg-destructive",
        iconBg: "bg-destructive/10",
        iconColor: "text-destructive",
        labelColor: "text-destructive",
        title: "Plano suspenso",
        description: "Novos registros estão bloqueados. Regularize sua assinatura para restaurar o acesso completo ao sistema.",
      };
    default:
      return null;
  }
}

function BillingStatusBadge() {
  const { session } = useAuth();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const autoOpened = useRef(false);
  const billing = session?.billing;

  useEffect(() => {
    if (!billing || billing.billingExempt || billing.canWrite || autoOpened.current) return;
    // Set the ref only when the timer fires so React StrictMode cleanup doesn't
    // prevent the second effect invocation from scheduling its own timer.
    const t = setTimeout(() => {
      if (!autoOpened.current) {
        autoOpened.current = true;
        setOpen(true);
      }
    }, 3000);
    return () => clearTimeout(t);
  }, [billing]);

  if (!billing || billing.billingExempt || billing.status === "trialing") return null;
  const cfg = billingStatusConfig(billing.status, billing.canWrite);
  if (!cfg) return null;

  const canClose = !cfg.blocked;

  const handleGoToBilling = () => {
    setOpen(false);
    navigate("/app/billing#checkout");
  };

  const modal = open
    ? createPortal(
        <div
          className="fixed inset-0 z-[9999] flex items-center justify-center p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="billing-modal-title"
        >
          {/* full-screen backdrop */}
          <div
            className="absolute inset-0 bg-black/70 backdrop-blur-md"
            onClick={canClose ? () => setOpen(false) : undefined}
          />

          {/* modal card */}
          <div className="relative z-10 w-full max-w-sm overflow-hidden rounded-2xl border border-border bg-card shadow-2xl">
            <div className={`h-1.5 w-full ${cfg.strip}`} />

            <div className="p-6">
              {canClose && (
                <button
                  onClick={() => setOpen(false)}
                  className="absolute right-4 top-6 rounded-md p-1 text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
                  type="button"
                  aria-label="Fechar popup"
                >
                  <X className="h-4 w-4" />
                </button>
              )}

              <div className="mb-4 flex items-center gap-4">
                <div className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-full ${cfg.iconBg}`}>
                  <CreditCard className={`h-6 w-6 ${cfg.iconColor}`} />
                </div>
                <div>
                  <h2 id="billing-modal-title" className="text-lg font-bold leading-tight">
                    {cfg.title}
                  </h2>
                  <p className={`text-sm font-semibold ${cfg.labelColor}`}>{cfg.label}</p>
                </div>
              </div>

              <p className="mb-6 text-sm leading-relaxed text-muted-foreground">
                {cfg.description}
              </p>

              <div className="flex flex-col gap-2">
                <Button className="w-full" onClick={handleGoToBilling}>
                  <CreditCard className="h-4 w-4" />
                  Regularizar agora
                </Button>
                {canClose && (
                  <Button variant="ghost" className="w-full" onClick={() => setOpen(false)}>
                    Fechar
                  </Button>
                )}
              </div>
            </div>
          </div>
        </div>,
        document.body
      )
    : null;

  return (
    <>
      <button
        onClick={() => setOpen(true)}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-full border border-border bg-background px-2 py-1 text-xs font-medium hover:bg-muted transition-colors"
        type="button"
        aria-label={`Status do plano: ${cfg.label}`}
      >
        <span className={`h-2 w-2 rounded-full ${cfg.dot}`} />
        <span className="hidden sm:inline text-muted-foreground">{cfg.label}</span>
      </button>
      {modal}
    </>
  );
}

type FarmEntry = { farmId: string; farmName: string; isCurrent: boolean };
type MyFarmsResponse = { farms: FarmEntry[]; totalFarms: number; maxFarms: number };

const FARM_AVATAR_COLORS = [
  "bg-emerald-600 text-white",
  "bg-blue-600 text-white",
  "bg-violet-600 text-white",
  "bg-amber-600 text-white",
  "bg-rose-600 text-white",
  "bg-teal-600 text-white",
  "bg-indigo-600 text-white",
  "bg-orange-600 text-white",
];

function farmInitials(name: string) {
  return name.split(/\s+/).map((w) => w[0]).filter(Boolean).slice(0, 2).join("").toUpperCase();
}

function farmAvatarColor(name: string) {
  const hash = name.split("").reduce((acc, ch) => acc + ch.charCodeAt(0), 0);
  return FARM_AVATAR_COLORS[hash % FARM_AVATAR_COLORS.length];
}

function FarmAvatar({ name, size = "md" }: { name: string; size?: "sm" | "md" }) {
  const cls = size === "sm" ? "h-7 w-7 text-xs" : "h-9 w-9 text-sm";
  return (
    <span className={`${cls} ${farmAvatarColor(name)} inline-flex shrink-0 items-center justify-center rounded-lg font-bold`}>
      {farmInitials(name)}
    </span>
  );
}

function FarmSwitcherButton({ collapsed }: { collapsed: boolean }) {
  const { session, fetchProtectedJson, switchFarm } = useAuth();
  const [open, setOpen] = useState(false);
  const [farms, setFarms] = useState<FarmEntry[] | null>(null);
  const [maxFarms, setMaxFarms] = useState<number>(-1);
  const [switching, setSwitching] = useState<string | null>(null);
  const [showAddForm, setShowAddForm] = useState(false);
  const [newFarmName, setNewFarmName] = useState("");
  const [adding, setAdding] = useState(false);
  const [addError, setAddError] = useState<string | null>(null);
  const farmName = session?.farmName ?? "";

  useEffect(() => {
    if (!open || farms != null) return;
    fetchProtectedJson<MyFarmsResponse>("/api/v1/my-farms")
      .then((data) => { setFarms(data.farms); setMaxFarms(data.maxFarms); })
      .catch(() => { setFarms([]); });
  }, [open, farms, fetchProtectedJson]);

  // Invalidate cache when another part of the app adds a farm (e.g. Settings page).
  useEffect(() => {
    function onFarmsUpdated() { setFarms(null); }
    window.addEventListener("farms-updated", onFarmsUpdated);
    return () => window.removeEventListener("farms-updated", onFarmsUpdated);
  }, []);

  const canAddFarm = maxFarms < 0 || (farms != null && farms.length < maxFarms);
  const hasMultiple = (session?.farmCount ?? 1) > 1 || (farms != null && farms.length > 1);

  async function handleSwitch(farmId: string) {
    setSwitching(farmId);
    try {
      await switchFarm(farmId);
      // Full reload so every page re-fetches with the new farmId in the token.
      // Tokens are persisted to localStorage before this point.
      window.location.reload();
    } finally {
      setSwitching(null);
    }
  }

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
      setNewFarmName("");
      setShowAddForm(false);
      setFarms((prev) => (prev != null ? [...prev, created] : null));
    } catch (error) {
      setAddError(describeApiError(error));
    } finally {
      setAdding(false);
    }
  }

  return (
    <Popover open={open} onOpenChange={(v) => { setOpen(v); if (!v) { setShowAddForm(false); setNewFarmName(""); setAddError(null); } }}>
      <PopoverTrigger asChild>
        <button
          type="button"
          aria-label={`Fazenda ativa: ${farmName}. Clique para trocar`}
          className={`flex w-full items-center gap-2.5 rounded-md px-2 py-2 transition-colors hover:bg-sidebar-accent ${
            collapsed ? "justify-center" : "justify-between"
          }`}
        >
          <div className="flex items-center gap-2.5 min-w-0">
            <FarmAvatar name={farmName} />
            {!collapsed && (
              <span className="min-w-0 truncate text-sm font-semibold text-sidebar-foreground">
                {farmName}
              </span>
            )}
          </div>
          {!collapsed && (
            <ChevronDown className={`h-4 w-4 shrink-0 text-sidebar-foreground/50 transition-transform ${open ? "rotate-180" : ""}`} />
          )}
        </button>
      </PopoverTrigger>

      <PopoverContent
        side="right"
        align="start"
        sideOffset={8}
        className="w-64 p-0"
      >
        <div className="px-3 py-2.5 border-b border-border">
          <p className="text-xs font-semibold text-muted-foreground uppercase tracking-wider">Suas fazendas</p>
        </div>

        <ul className="py-1">
          {farms === null && (
            <li className="px-3 py-2.5">
              <div className="flex items-center gap-3">
                <div className="h-7 w-7 rounded-lg bg-muted animate-pulse shrink-0" />
                <div className="h-3 w-32 rounded bg-muted animate-pulse" />
              </div>
            </li>
          )}
          {(farms ?? []).map((farm) => (
            <li key={farm.farmId}>
              <button
                type="button"
                disabled={farm.isCurrent || switching != null}
                onClick={() => { if (!farm.isCurrent) void handleSwitch(farm.farmId); }}
                className={`flex w-full items-center gap-3 px-3 py-2.5 text-sm transition-colors ${
                  farm.isCurrent
                    ? "cursor-default"
                    : "hover:bg-muted cursor-pointer"
                }`}
              >
                <FarmAvatar name={farm.farmName} size="sm" />
                <span className="flex-1 min-w-0 truncate text-left font-medium">
                  {farm.farmName}
                </span>
                {farm.isCurrent && <Check className="h-4 w-4 shrink-0 text-primary" />}
                {switching === farm.farmId && (
                  <span className="text-xs text-muted-foreground">Entrando…</span>
                )}
              </button>
            </li>
          ))}
        </ul>

        {session?.role === "admin" && canAddFarm && (
          <div className="border-t border-border py-1">
            {!showAddForm ? (
              <button
                type="button"
                onClick={() => setShowAddForm(true)}
                className="flex w-full items-center gap-2.5 px-3 py-2.5 text-sm text-muted-foreground hover:bg-muted hover:text-foreground transition-colors"
              >
                <Plus className="h-4 w-4 shrink-0" />
                Adicionar fazenda
              </button>
            ) : (
              <div className="px-3 py-3 space-y-2">
                <Input
                  autoFocus
                  maxLength={255}
                  onChange={(e) => { setNewFarmName(e.target.value); setAddError(null); }}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") void handleAddFarm();
                    if (e.key === "Escape") { setShowAddForm(false); setNewFarmName(""); setAddError(null); }
                  }}
                  placeholder="Nome da fazenda"
                  value={newFarmName}
                />
                {addError != null && <p className="text-xs text-destructive">{addError}</p>}
                <div className="flex gap-2">
                  <Button
                    disabled={adding || !newFarmName.trim()}
                    onClick={() => void handleAddFarm()}
                    size="sm"
                    type="button"
                  >
                    {adding ? "Salvando..." : "Salvar"}
                  </Button>
                  <Button
                    onClick={() => { setShowAddForm(false); setNewFarmName(""); setAddError(null); }}
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
        )}

        {!hasMultiple && !canAddFarm && (
          <div className="border-t border-border px-3 py-2.5">
            <p className="text-xs text-muted-foreground">
              Seu plano não permite mais fazendas.{" "}
              <Link to="/app/billing" onClick={() => setOpen(false)} className="text-primary hover:underline">
                Fazer upgrade
              </Link>
            </p>
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
}

const MOBILE_BREAKPOINT = 768;

function isMobileViewport() {
  return typeof window !== "undefined" && window.innerWidth < MOBILE_BREAKPOINT;
}

export function AdminShellLayout() {
  const { logout, session } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [collapsed, setCollapsed] = useState(isMobileViewport);

  const onboarding = useOnboardingStatus();
  const showWizard = session?.role === "admin" && onboarding.shouldShow && onboarding.status != null;

  const [tourPending, setTourPending] = useState(
    () => localStorage.getItem("curralzap.admin.tour_pending") === "true"
  );
  const [runTour, setRunTour] = useState(false);

  useEffect(() => {
    if (tourPending && !showWizard && !onboarding.loading) {
      localStorage.removeItem("curralzap.admin.tour_pending");
      setTourPending(false);
      setCollapsed(false);
      setRunTour(true);
    }
  }, [tourPending, showWizard, onboarding.loading]);

  const roleLabel = ROLE_LABELS[session?.role ?? ""] ?? "User";
  const roleColor = ROLE_COLORS[session?.role ?? ""] ?? "bg-muted text-muted-foreground";

  return (
    <div className="flex h-screen bg-background overflow-hidden">
      <motion.aside
        animate={{ width: collapsed ? 64 : 240 }}
        transition={{ duration: 0.2, ease: "easeInOut" }}
        data-collapsed={collapsed}
        className="h-screen border-r border-sidebar-border bg-sidebar flex flex-col shrink-0 overflow-hidden"
      >
        <div className="p-3 border-b border-sidebar-border flex items-center justify-between h-14">
          {!collapsed && (
            <div className="flex items-center gap-2">
              <BrandLogo className="w-8 h-8 shrink-0 text-sidebar-foreground" />
              <span className="font-heading font-bold text-lg tracking-tight text-sidebar-foreground whitespace-nowrap">
                ManejoPrime
              </span>
            </div>
          )}
          <button
            aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
            className="p-1.5 rounded-md hover:bg-sidebar-accent text-sidebar-foreground/60 hover:text-sidebar-foreground transition-colors shrink-0"
            onClick={() => setCollapsed((prev) => !prev)}
            type="button"
          >
            {collapsed ? <PanelLeftOpen className="w-5 h-5" /> : <PanelLeftClose className="w-5 h-5" />}
          </button>
        </div>

        <div className="px-1.5 py-2 border-b border-sidebar-border">
          <FarmSwitcherButton collapsed={collapsed} />
        </div>

        <nav aria-label="Admin shell" className="flex-1 overflow-y-auto py-2 px-1.5">
          {NAV_GROUPS.map((group) => {
            const visibleItems = group.items.filter((item) => hasAnyPermission(session, item.requiredPermissions));
            if (visibleItems.length === 0) return null;
            return (
              <div key={group.label} className="mb-3" data-tour={group.tourId}>
                {!collapsed && (
                  <p className="px-2.5 pt-1 pb-0.5 text-[10px] font-semibold uppercase tracking-wider text-sidebar-foreground/40">
                    {group.label}
                  </p>
                )}
                {visibleItems.map((item) => {
                  const Icon = item.icon;
                  return (
                    <NavLink
                      className={({ isActive }) =>
                        `flex items-center gap-2.5 rounded-md text-sm px-2.5 py-2.5 mb-0.5 transition-colors ${
                          isActive
                            ? "bg-sidebar-accent text-sidebar-primary font-medium"
                            : "text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground"
                        }`
                      }
                      end={item.path === "/app" || item.path === "/app/animals"}
                      key={item.path}
                      to={item.path}
                    >
                      <Icon className="w-5 h-5 shrink-0" />
                      {!collapsed && <span className="whitespace-nowrap overflow-hidden">{item.label}</span>}
                    </NavLink>
                  );
                })}
              </div>
            );
          })}
        </nav>

        <div className="p-1.5 border-t border-sidebar-border">
          <button
            className="flex items-center gap-2.5 w-full rounded-md text-sm px-2.5 py-2.5 text-sidebar-foreground/70 hover:bg-sidebar-accent hover:text-sidebar-foreground transition-colors"
            onClick={() => void logout()}
            type="button"
          >
            <LogOut className="w-5 h-5 shrink-0" />
            {!collapsed && <span>Sair</span>}
          </button>
        </div>
      </motion.aside>

      <div className="flex flex-col flex-1 overflow-hidden min-w-0">
        <header className="h-14 border-b border-border bg-card/80 backdrop-blur-sm flex items-center justify-between gap-3 px-3 sm:px-6 shrink-0 sticky top-0 z-20">
          <div className="flex items-center gap-2 min-w-0" />
          <div className="flex shrink-0 items-center gap-2 sm:gap-3">
            {session?.billing?.status === "trialing" && session.billing.currentPeriodEnd != null && (
              <TrialChip periodEnd={session.billing.currentPeriodEnd} />
            )}
            <BillingStatusBadge />
            {session?.role != null && (
              <span
                className={`inline-flex shrink-0 items-center gap-1.5 rounded-full px-2 py-1 text-sm font-semibold sm:px-2.5 ${roleColor}`}
              >
                <Shield className="w-4 h-4" />
                <span className="hidden sm:inline">{roleLabel}</span>
              </span>
            )}
            {toggleTheme != null && (
              <button
                aria-label="Alternar modo escuro"
                className="p-1.5 rounded-md hover:bg-muted text-muted-foreground hover:text-foreground transition-colors"
                onClick={toggleTheme}
                type="button"
              >
                {theme === "dark" ? <Sun className="w-5 h-5" /> : <Moon className="w-5 h-5" />}
              </button>
            )}
          </div>
        </header>

        {session?.billing?.status === "trialing" && session.billing.currentPeriodEnd != null && (
          <TrialBanner periodEnd={session.billing.currentPeriodEnd} />
        )}

        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>
      <Toaster />
      <FloatingWhatsAppBubbles />
      <AppTour steps={SIDEBAR_TOUR_STEPS} run={runTour} onFinish={() => setRunTour(false)} />
      {showWizard && (
        <OnboardingWizardModal
          status={onboarding.status!}
          onDismiss={onboarding.dismiss}
          onStatusRefresh={onboarding.refetch}
        />
      )}
    </div>
  );
}
