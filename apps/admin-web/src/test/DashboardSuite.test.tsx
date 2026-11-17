import { screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { DashboardPage } from "../features/dashboard/DashboardPage";
import {
  buildSession,
  jsonResponse,
  renderAppRoutes,
  renderWithAuth,
  setMockFetch,
  setPendingFetch
} from "./support/adminWebTestSupport";
import { adminWebRoleScenarios } from "./support/roleMatrix";

const DASHBOARD_SESSION = buildSession({ role: "admin", permissions: ["dashboard.read"] });

const KPI_RESPONSE = {
  totalAnimals: 120,
  totalAnimalsChangePct: 5.2,
  activeAnimals: 115,
  activeAnimalsChangePct: -2.1,
  lotsCount: 5,
  lotsCountChangePct: 0,
  activeUsers: 8,
  activeUsersChangePct: 12.5,
  avgDailyGain: 1.32,
  avgDailyGainChangePct: 8.2,
  statusBreakdown: { active: 115, sold: 3, dead: 1, transferred: 1 },
  alerts: [],
  shortcuts: [],
};

const WEIGHT_TREND_RESPONSE = [
  { date: "2026-05-01", avgWeight: 410, avgDailyGain: 1.1 },
  { date: "2026-05-02", avgWeight: 425, avgDailyGain: 1.5 },
];

const LOT_BREAKDOWN_RESPONSE = [
  { lotName: "Engorda 1", animalCount: 50 },
  { lotName: "Engorda 2", animalCount: 45 },
  { lotName: "Cria", animalCount: 25 },
];

const REPORTS_DASHBOARD_RESPONSE = {
  totalAnimals: 48,
  totalAnimalsChangePct: 0,
  activeAnimals: 43,
  activeAnimalsChangePct: 0,
  lotsCount: 6,
  lotsCountChangePct: 0,
  activeUsers: 5,
  activeUsersChangePct: 0,
  avgDailyGain: null,
  avgDailyGainChangePct: 0,
  statusBreakdown: { active: 43, sold: 3, dead: 1, transferred: 1 },
  alerts: [{ type: "occurrence", title: "Open health occurrences", count: 4 }],
  shortcuts: ["animals", "users", "settings"]
};

const OCCURRENCES_RESPONSE = {
  items: [
    {
      id: "occurrence-1",
      animalId: "animal-1",
      animalIdentifier: "347",
      animalName: "Trovao",
      type: "health",
      severity: "critical",
      status: "open",
      description: "High fever after transport.",
      photoUrl: null,
      source: "app",
      recordedBy: "admin-user",
      resolvedBy: null,
      createdAt: "2026-05-01T08:00:00Z",
      updatedAt: "2026-05-01T09:00:00Z"
    }
  ],
  total: 1,
  limit: 20,
  offset: 0
};

function setupReportsMock() {
  const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
    const url = typeof input === "string" ? input : input.toString();
    if (url.endsWith("/api/v1/dashboard")) return json(REPORTS_DASHBOARD_RESPONSE);
    if (url.includes("/api/v1/occurrences")) return json(OCCURRENCES_RESPONSE);
    throw new Error(`Unexpected: GET ${url}`);
  });
  vi.stubGlobal("fetch", fetchMock);
  return fetchMock;
}

function json(body: unknown, init: ResponseInit = { status: 200 }) {
  return Promise.resolve(new Response(JSON.stringify(body), init));
}

describe("dashboard migration", () => {
  it("KPI cards render totalAnimals, activeAnimals, lotsCount, activeUsers", async () => {
    setMockFetch(
      jsonResponse(KPI_RESPONSE),
      jsonResponse(WEIGHT_TREND_RESPONSE),
      jsonResponse(LOT_BREAKDOWN_RESPONSE)
    );
    renderAppRoutes({ session: DASHBOARD_SESSION });

    await waitFor(() => {
      expect(screen.getByText("Total de Animais")).toBeInTheDocument();
    });
    expect(screen.getByText("GMD Diário")).toBeInTheDocument();
    expect(screen.getByText("1.32 kg/dia")).toBeInTheDocument();
    expect(screen.getByText("120")).toBeInTheDocument();
    expect(screen.getByText("Animais Ativos")).toBeInTheDocument();
    expect(screen.getByText("115")).toBeInTheDocument();
    expect(screen.getByText("Lotes")).toBeInTheDocument();
    expect(screen.getByText("5")).toBeInTheDocument();
    expect(screen.getByText("Usuários Ativos")).toBeInTheDocument();
    expect(screen.getByText("8")).toBeInTheDocument();
  });

  it("weight-trend LineChart renders data from /api/v1/dashboard/weight-trend", async () => {
    setMockFetch(
      jsonResponse(KPI_RESPONSE),
      jsonResponse(WEIGHT_TREND_RESPONSE),
      jsonResponse(LOT_BREAKDOWN_RESPONSE)
    );
    renderAppRoutes({ session: DASHBOARD_SESSION });

    await waitFor(() => {
      expect(screen.getByTestId("recharts-line-chart")).toHaveAttribute("data-point-count", "2");
    });
  });

  it("lot-breakdown BarChart renders data from /api/v1/dashboard/lot-breakdown", async () => {
    setMockFetch(
      jsonResponse(KPI_RESPONSE),
      jsonResponse(WEIGHT_TREND_RESPONSE),
      jsonResponse(LOT_BREAKDOWN_RESPONSE)
    );
    renderAppRoutes({ session: DASHBOARD_SESSION });

    await waitFor(() => {
      expect(screen.getByTestId("recharts-bar-chart")).toHaveAttribute("data-point-count", "3");
    });
  });

  it("shows skeleton while KPI fetch is in flight", async () => {
    setPendingFetch();
    renderAppRoutes({ session: DASHBOARD_SESSION });

    await waitFor(() => {
      expect(screen.getByTestId("kpi-skeleton")).toBeInTheDocument();
    });
    expect(screen.queryByText("Total Animals")).not.toBeInTheDocument();
  });
});

describe("admin dashboard page", () => {
  it("renders KPI cards with data from /api/v1/dashboard", async () => {
    const session = buildSession({
      role: "admin",
      permissions: ["dashboard.read"],
      farmName: "Fazenda Teste"
    });

    setMockFetch(
      jsonResponse({
        totalAnimals: 30,
        activeAnimals: 27,
        lotsCount: 4,
        activeUsers: 3,
        statusBreakdown: { active: 27, sold: 2, dead: 1, transferred: 0 },
        alerts: [],
        shortcuts: []
      }),
      jsonResponse([]),
      jsonResponse([])
    );

    renderWithAuth(<DashboardPage />, { session });

    await waitFor(() => {
      expect(screen.getByText("Total de Animais")).toBeInTheDocument();
    });
    expect(screen.getByText("30")).toBeInTheDocument();
    expect(screen.getByText("Animais Ativos")).toBeInTheDocument();
    expect(screen.getByText("27")).toBeInTheDocument();
    expect(screen.getByText("Lotes")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("Usuários Ativos")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
  });

  it("shows skeleton while KPI data is loading", async () => {
    const session = buildSession({ permissions: ["dashboard.read"] });
    setPendingFetch();

    renderWithAuth(<DashboardPage />, { session });

    await waitFor(() => {
      expect(screen.getByTestId("kpi-skeleton")).toBeInTheDocument();
    });
    expect(screen.queryByText("Total Animals")).not.toBeInTheDocument();
  });
});

describe("reports page migration", () => {
  it("renders page heading 'Reports' as h1", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupReportsMock();
    renderAppRoutes({ initialEntries: ["/app/reports"], session: admin.session });

    await waitFor(() => expect(screen.getByRole("heading", { level: 1, name: "Relatórios" })).toBeInTheDocument());
  });

  it("shows metric cards with farm totals and occurrence card data", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setupReportsMock();
    renderAppRoutes({ initialEntries: ["/app/reports"], session: admin.session });

    await waitFor(() => expect(screen.getByText("48")).toBeInTheDocument());
    expect(screen.getAllByText("Animais").length).toBeGreaterThan(0);
    expect(screen.getByText("Resumo da fazenda")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByText("High fever after transport.")).toBeInTheDocument());
  });

  it("shows skeleton while dashboard is loading", async () => {
    const admin = adminWebRoleScenarios.find((s) => s.name === "admin")!;
    setPendingFetch();
    renderAppRoutes({ initialEntries: ["/app/reports"], session: admin.session });

    await waitFor(() => expect(screen.getByTestId("reports-skeleton")).toBeInTheDocument());
    expect(screen.queryByText("Resumo da fazenda")).not.toBeInTheDocument();
  });
});
