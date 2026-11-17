import { SessionMe } from "../../features/auth/AuthContext";

type AdminWebRoleScenario = {
  name: string;
  session: SessionMe;
  visibleNavItems: string[];
  hiddenNavItems: string[];
};

const WORKER_PERMISSIONS = ["dashboard.read", "animals.read", "lots.read", "weighings.write", "occurrences.write"];
const VETERINARIAN_PERMISSIONS = [
  "dashboard.read",
  "animals.read",
  "lots.read",
  "weighings.write",
  "occurrences.write",
  "vaccines.write",
  "medications.write",
  "reports.health.read"
];
const ADMIN_PERMISSIONS = [
  "dashboard.read",
  "animals.read",
  "animals.write",
  "lots.read",
  "lots.write",
  "weighings.write",
  "occurrences.write",
  "vaccines.write",
  "medications.write",
  "feeding.write",
  "users.read",
  "users.write",
  "settings.read",
  "settings.write",
  "offers.read",
  "offers.write",
  "reports.read"
];

export const adminWebRoleScenarios: AdminWebRoleScenario[] = [
  {
    name: "worker",
    session: buildSession("worker", "Worker Matrix", WORKER_PERMISSIONS),
    visibleNavItems: ["Painel", "Animais", "Saúde"],
    hiddenNavItems: ["Ofertas", "Relatórios", "Usuários", "Configurações"]
  },
  {
    name: "veterinarian",
    session: buildSession("veterinarian", "Vet Matrix", VETERINARIAN_PERMISSIONS),
    visibleNavItems: ["Painel", "Animais", "Saúde", "Relatórios"],
    hiddenNavItems: ["Ofertas", "Usuários", "Configurações"]
  },
  {
    name: "admin",
    session: buildSession("admin", "Admin Matrix", ADMIN_PERMISSIONS),
    visibleNavItems: ["Painel", "Animais", "Saúde", "Ofertas", "Relatórios", "Usuários", "Configurações"],
    hiddenNavItems: []
  }
];

export const forbiddenAdminWebRouteScenarios = [
  {
    name: "worker",
    session: adminWebRoleScenarios[0].session,
    initialEntry: "/app/users",
    blockedCopy: "Gerenciar usuários da fazenda."
  },
  {
    name: "veterinarian",
    session: adminWebRoleScenarios[1].session,
    initialEntry: "/app/settings",
    blockedCopy: "Configurações da fazenda."
  }
] as const;

export function buildDashboardResponse(shortcuts: string[] = ["animals"]) {
  return {
    totalAnimals: 18,
    activeAnimals: 17,
    lotsCount: 3,
    activeUsers: 2,
    statusBreakdown: {
      active: 17,
      sold: 1,
      dead: 0,
      transferred: 0
    },
    alerts: [],
    shortcuts
  };
}

function buildSession(role: SessionMe["role"], name: string, permissions: string[]): SessionMe {
  return {
    userId: `${role}-user`,
    name,
    email: `${role}@example.com`,
    phone: "5511999990001",
    role,
    farmId: "farm-1",
    farmName: "Fazenda Matrix",
    permissions
  };
}
